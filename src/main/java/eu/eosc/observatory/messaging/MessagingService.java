package eu.eosc.observatory.messaging;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.GroupService;
import eu.eosc.observatory.service.UserService;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import org.springframework.context.annotation.Lazy;
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

    private final Set<String> connectedUsers = Collections.synchronizedSet(new HashSet<>());

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessagingService(@Lazy MessagingSystemController messagingSystemController,
                            GroupService groupService,
                            UserService userService,
                            SimpMessagingTemplate simpMessagingTemplate) {
        this.messagingSystemController = messagingSystemController;
        this.groupService = groupService;
        this.userService = userService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @EventListener
    private void subscribe(SessionConnectEvent event) {
        if (event.getUser() != null) {
            this.connectedUsers.add(User.getId((OAuth2AuthenticationToken) event.getUser()));
        }
    }

    @EventListener
    public void unsubscribe(SessionDisconnectEvent event) {
        if (event.getUser() != null) {
            this.connectedUsers.remove(User.getId((OAuth2AuthenticationToken) event.getUser()));
        }
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

}
