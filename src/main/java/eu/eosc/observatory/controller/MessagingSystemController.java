package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.UserService;
import gr.athenarc.messaging.config.MessagingClientProperties;
import gr.athenarc.messaging.controller.MessagingController;
import gr.athenarc.messaging.controller.RestApiPaths;
import gr.athenarc.messaging.domain.Message;
import gr.athenarc.messaging.domain.TopicThread;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.dto.UnreadThreads;
import gr.athenarc.recaptcha.annotation.ReCaptcha;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class MessagingSystemController extends MessagingController {

    private final UserService userService;

    public MessagingSystemController(MessagingClientProperties messagingClientProperties,
                                     UserService userService) {
        super(messagingClientProperties);
        this.userService = userService;
    }

    @ReCaptcha("#recaptcha")
    @PostMapping(RestApiPaths.THREADS + "/public")
    public Mono<ThreadDTO> addExternal(@RequestHeader("g-recaptcha-response") String recaptcha, @RequestBody ThreadDTO thread) {
        return super.add(thread);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME:
    public Mono<ThreadDTO> get(String threadId, String email) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.get(threadId, email);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Mono<ThreadDTO> add(ThreadDTO thread) {
        return super.add(thread);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ThreadDTO> update(String threadId, TopicThread topicThread) {
        return super.update(threadId, topicThread);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> delete(String threadId) {
        return super.delete(threadId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Mono<UnreadThreads> searchUnreadThreads(List<String> groups, String email) {
        return super.searchUnreadThreads(groups, email); // 1 FIXME: returns 0 and empty group list
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME: check if user is member of provided groupId
    public Flux<ThreadDTO> searchInbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInbox(groupId, regex, email, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME: check if user is member of provided groups
    // TODO: remove groups argument and find it by authenticated user
    public Flux<ThreadDTO> searchInboxUnread(List<String> groups, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInboxUnread(groups, email, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME: check if user is member of provided groupId
    // TODO: check if email is matching with user emails
    public Flux<ThreadDTO> searchOutbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        if (userIsMemberOfGroup(User.getId(SecurityContextHolder.getContext().getAuthentication()), groupId)) {
            return super.searchOutbox(groupId, regex, email, sortBy, direction, page, size);
        }
        return Flux.just();
    }

    @Override
    @PreAuthorize("isAuthenticated() and authentication.principal.getAttribute('email') == #message.from.email")
    public Mono<ThreadDTO> addMessage(String threadId, Message message, boolean anonymous) {
        return super.addMessage(threadId, message, anonymous);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // This method changes message status for every member.
    // FIXME: check if user is member of this thread.
    public Mono<ThreadDTO> readMessage(String threadId, String messageId, boolean read, String userId) {
        // ignore given userId
        userId = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.readMessage(threadId, messageId, read, userId);
//        return super.get(threadId).map(t -> t.getFrom()) super.readMessage(threadId, messageId, read);
    }

    public boolean userIsMemberOfGroup(String userId, String groupId) {
        UserInfo info = userService.getUserInfo(userId);
        Set<String> groups = new HashSet<>();
        groups.addAll(info.getCoordinators().stream().map(Coordinator::getId).collect(Collectors.toSet()));
        groups.addAll(info.getStakeholders().stream().map(Stakeholder::getId).collect(Collectors.toSet()));
        return groups.contains(groupId);
    }
}
