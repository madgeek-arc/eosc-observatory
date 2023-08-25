package eu.eosc.observatory.aspect;

import eu.eosc.observatory.controller.MessagingSystemController;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.sse.EmitterMessage;
import eu.eosc.observatory.sse.EmitterService;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.dto.UnreadThreads;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Aspect
@Component
public class MessageUpdatesAspect {

    private final EmitterService emitterService;
    private final CoordinatorService coordinatorService;
    private final StakeholderService stakeholderService;
    private final MessagingSystemController messagingSystemController;

    public MessageUpdatesAspect(EmitterService emitterService,
                                CoordinatorService coordinatorService,
                                StakeholderService stakeholderService,
                                MessagingSystemController messagingSystemController) {
        this.emitterService = emitterService;
        this.coordinatorService = coordinatorService;
        this.stakeholderService = stakeholderService;
        this.messagingSystemController = messagingSystemController;
    }

    @AfterReturning(value = "(execution(* eu.eosc.observatory.controller.MessagingSystemController.addExternal(..)))" +
            " || (execution(* eu.eosc.observatory.controller.MessagingSystemController.add(..)))" +
            " || (execution(* eu.eosc.observatory.controller.MessagingSystemController.addMessage(..)))", returning = "threadDTOMono")
    public void unreadMessagesNotification(Mono<ThreadDTO> threadDTOMono) {
        ThreadDTO threadDTO = threadDTOMono.block();
        List<MessageDTO> messages = threadDTO.getMessages();
        MessageDTO message = messages.get(messages.size() - 1);
        List<Correspondent> to = message.getTo();
        Set<String> userEmails = new HashSet<>();
        for (Correspondent entry : to) {
            if (entry.getEmail() != null) {
                userEmails.add(entry.getEmail());
            }
            if (entry.getGroupId() != null) {
                userEmails.addAll(getUsers(entry.getGroupId()));
            }
        }

        for (String email : userEmails) {
            UnreadThreads unread = messagingSystemController.getUnread(new ArrayList<>(), email);
            EmitterMessage emitterMessage = new EmitterMessage();
            emitterMessage.setData(unread);
            emitterMessage.setEventName("unread-threads");
            emitterService.sendDataToUserEmitters(email, emitterMessage);
        }
    }

    private List<String> getUsers(String groupId) {
        Set<String> members = new HashSet<>();
        if (groupId.startsWith("sh-")) {
            Stakeholder sh = stakeholderService.get(groupId);
            members.addAll(sh.getManagers());
            members.addAll(sh.getContributors());
        } else if (groupId.startsWith("co-")) {
            members.addAll(coordinatorService.get(groupId).getMembers());
        }
        return members.stream().toList();
    }
}
