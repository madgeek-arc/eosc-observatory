/*
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.messaging;

import eu.openaire.observatory.domain.Coordinator;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserInfo;
import eu.openaire.observatory.service.GroupService;
import eu.openaire.observatory.service.UserService;
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
        for (String email : userEmails(threadDTO)) {
            if (connectedUsers.contains(email)) {
                messagingSystemController.getUnread(email).subscribe(unread ->
                        simpMessagingTemplate.convertAndSend("/topic/messages/inbox/unread/" + email, unread)
                );
            }
        }
    }

    public void incomingMailNotification(ThreadDTO threadDTO) {
        for (String email : userEmails(threadDTO)) {
            if (connectedUsers.contains(email)) {
                simpMessagingTemplate.convertAndSend("/topic/messages/inbox/notification/" + email, threadDTO);
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

    private Set<String> userEmails(ThreadDTO threadDTO) {
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
        return userEmails;
    }
}
