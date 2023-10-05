package eu.eosc.observatory.messaging;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.GroupService;
import eu.eosc.observatory.service.UserService;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.mailer.client.service.MailClient;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessagingService {


    private final MessagingSystemController messagingSystemController;
    private final GroupService groupService;
    private final UserService userService;
    private final MailClient mailClient;
    private final Configuration configuration;

    private final Set<String> connectedUsers = Collections.synchronizedSet(new HashSet<>());

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessagingService(@Lazy MessagingSystemController messagingSystemController,
                            GroupService groupService,
                            UserService userService,
                            MailClient mailClient,
                            SimpMessagingTemplate simpMessagingTemplate,
                            Configuration configuration) {
        this.messagingSystemController = messagingSystemController;
        this.groupService = groupService;
        this.userService = userService;
        this.mailClient = mailClient;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.configuration = configuration;
    }

    @EventListener
    private void subscribe(SessionConnectEvent event) {
        this.connectedUsers.add(User.getId((OAuth2AuthenticationToken) event.getUser()));
    }

    @EventListener
    public void unsubscribe(SessionDisconnectEvent event) {
        this.connectedUsers.remove(User.getId((OAuth2AuthenticationToken) event.getUser()));
    }

    public void updateUnread(ThreadDTO threadDTO) {
        List<MessageDTO> messages = threadDTO.getMessages();
        MessageDTO message = messages.get(messages.size() - 1);
        List<Correspondent> to = message.getTo();
        Set<String> userEmails = new HashSet<>();
        for (Correspondent entry : to) {
            if (entry.getEmail() != null) {
                userEmails.add(entry.getEmail());
            }
            if (entry.getGroupId() != null) {
                userEmails.addAll(groupService.getUserIds(entry.getGroupId()));
            }
        }
        this.updateUnreadOfUser(userEmails);
    }

    public void updateUnreadOfUser(Set<String> userEmails) {
        for (String email : userEmails) {
            if (connectedUsers.contains(email)) {
                messagingSystemController.getUnread(email).subscribe(unread -> {
                            simpMessagingTemplate.convertAndSend("/topic/messages/inbox/unread/" + email, unread);
                        }
                );
            }
        }
    }

    public List<String> filterUserGroups(String email, List<String> groups) {
        List<String> userGroups = new ArrayList<>();
        UserInfo info = userService.getUserInfo(email);
        userGroups.addAll(info.getStakeholders().stream().map(Stakeholder::getId).collect(Collectors.toList()));
        userGroups.addAll(info.getCoordinators().stream().map(Coordinator::getId).collect(Collectors.toList()));
        if (!groups.isEmpty()) {
            userGroups = userGroups.stream().distinct().filter(groups::contains).collect(Collectors.toList());
        }
        return userGroups;
    }

    public void sendEmails(ThreadDTO thread) {
        // TODO: refactor
        MessageDTO messageDTO = thread.getMessages().get(thread.getMessages().size() - 1);
        StringWriter stringWriter = new StringWriter();
        Map<String, String> data = new HashMap<>();
        data.put("subject", thread.getSubject());
        data.put("content", messageDTO.getBody());
        data.put("user", messageDTO.getFrom().getName() != null ? messageDTO.getFrom().getName() : messageDTO.getFrom().getGroupId());
        try {
            configuration.getTemplate("message.ftlh").process(data, stringWriter);
        } catch (Exception e) {

        }

        List<String> ccEmails = new ArrayList<>();
        List<String> bccEmails = messageDTO
                .getTo()
                .stream()
                .map(correspondent -> {
                    if (correspondent.getGroupId() == null) {
                        return List.of(correspondent.getEmail());
                    }
                    return groupService.getUserIds(correspondent.getGroupId());
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (!messageDTO.isAnonymousSender()) {
            ccEmails.add(messageDTO.getFrom().getEmail());
        }
        EmailMessage email = new EmailMessage.EmailBuilder()
                .setFrom("no-reply@openaire.eu")
//                .setCc(ccEmails)
                .setBcc(bccEmails)
                .setSubject(thread.getSubject())
                .setText(stringWriter.getBuffer().toString())
                .setHtml(true)
                .build();
        mailClient.sendMail(email);
    }
}
