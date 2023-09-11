package eu.eosc.observatory.messaging;

import eu.eosc.observatory.service.GroupService;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Aspect
@Controller
public class MessageUpdatesAspect {

    private final GroupService groupService;
    private final MessagingService messagingService;


    public MessageUpdatesAspect(GroupService groupService,
                                MessagingService messagingService) {
        this.groupService = groupService;
        this.messagingService = messagingService;
    }

    @AfterReturning(value = "(execution(* eu.eosc.observatory.messaging.MessagingSystemController.addExternal(..)))" +
            " || (execution(* eu.eosc.observatory.messaging.MessagingSystemController.add(..)))" +
            " || (execution(* eu.eosc.observatory.messaging.MessagingSystemController.addMessage(..)))", returning = "threadDTOMono")
    public void unreadMessagesNotification(Mono<ThreadDTO> threadDTOMono) {
        // update user Unread Messages after mono returns successfully.
        threadDTOMono.doOnSuccess(this::updateUnread);
    }

    private void updateUnread(ThreadDTO threadDTO) {
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

        messagingService.updateUnreadOfUser(userEmails);
    }
}
