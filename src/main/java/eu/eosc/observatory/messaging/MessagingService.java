package eu.eosc.observatory.messaging;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.GroupService;
import eu.eosc.observatory.service.UserService;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.dto.UnreadThreads;
import gr.athenarc.messaging.mailer.client.service.MailClient;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessagingService {


    private final MessagingSystemController messagingSystemController;
    private final GroupService groupService;
    private final UserService userService;
    private final MailClient mailClient;

    private final Set<String> connectedUsers = Collections.synchronizedSet(new HashSet<>());

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessagingService(MessagingSystemController messagingSystemController,
                            GroupService groupService,
                            UserService userService,
                            MailClient mailClient,
                            SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingSystemController = messagingSystemController;
        this.groupService = groupService;
        this.userService = userService;
        this.mailClient = mailClient;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @EventListener
    private void subscribe(SessionConnectEvent event) {
        this.connectedUsers.add(User.getId((OAuth2AuthenticationToken) event.getUser()));
    }

    @EventListener
    public void unsubscribe(SessionDisconnectEvent event) {
        this.connectedUsers.remove(User.getId((OAuth2AuthenticationToken) event.getUser()));
    }

    public void updateUnreadOfUser(Set<String> userEmails) {
        for (String email : userEmails) {
            if (connectedUsers.contains(email)) {
                UnreadThreads unread = messagingSystemController.getUnread(email);
                simpMessagingTemplate.convertAndSend("/topic/messages/inbox/unread/" + email, unread);
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
        MessageDTO messageDTO = thread.getMessages().get(thread.getMessages().size() - 1);
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
                .setFrom("")
                .setCc(ccEmails)
                .setBcc(bccEmails)
                .setSubject(thread.getSubject())
                .setText(messageDTO.getBody())
                .build();
        mailClient.sendMail(email);
    }
}
