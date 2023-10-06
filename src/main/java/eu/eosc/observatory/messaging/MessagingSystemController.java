package eu.eosc.observatory.messaging;

import eu.eosc.observatory.domain.User;
import gr.athenarc.catalogue.exception.ServerError;
import gr.athenarc.messaging.config.MessagingClientProperties;
import gr.athenarc.messaging.controller.MessagingController;
import gr.athenarc.messaging.controller.RestApiPaths;
import gr.athenarc.messaging.domain.Message;
import gr.athenarc.messaging.domain.TopicThread;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.dto.UnreadThreads;
import gr.athenarc.recaptcha.annotation.ReCaptcha;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@RestController
public class MessagingSystemController extends MessagingController {

    private final MessagingService messagingService;
    private final EmailOperations emailOperations;

    public MessagingSystemController(MessagingClientProperties messagingClientProperties,
                                     MessagingService messagingService,
                                     EmailOperations emailOperations) {
        super(messagingClientProperties);
        this.messagingService = messagingService;
        this.emailOperations = emailOperations;
    }

    @MessageMapping("messages/inbox/unread/{userId}")
    @SendTo("/topic/messages/inbox/unread/{userId}")
    public UnreadThreads userUnreadThreadsWS(@DestinationVariable("userId") String userId,
                                             @Parameter(hidden = true) Authentication auth) {
        if (!userId.equals(User.getId(auth))) {
            throw new AccessDeniedException(String.format("%nUser ID: %s%nAuthenticated user ID: %s", userId, User.getId(auth)));
        }
        return getUnread(userId).block();
    }

    @ReCaptcha("#recaptcha")
    @PostMapping(RestApiPaths.THREADS + "/public")
    public Mono<ThreadDTO> addExternal(@RequestHeader("g-recaptcha-response") String recaptcha, @RequestBody ThreadDTO thread) {
        return super.add(thread).doOnNext(t ->
                Mono.fromRunnable(() -> {
                            messagingService.updateUnread(t);
                            emailOperations.sendEmails(t);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
        );
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    public Mono<ThreadDTO> get(String threadId, String email, String groupId) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.get(threadId, email, groupId);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #thread.from.groupId)")
    public Mono<ThreadDTO> add(ThreadDTO thread) {
        return super.add(thread).doOnNext(t ->
                Mono.fromRunnable(() -> {
                            messagingService.updateUnread(t);
                            emailOperations.sendEmails(t);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
        );
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
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groups)")
    public Mono<UnreadThreads> searchUnreadThreads(@RequestParam(defaultValue = "") List<String> groups, String email) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        groups = messagingService.filterUserGroups(email, groups);
        return super.searchUnreadThreads(groups, email);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    public Flux<ThreadDTO> searchInbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInbox(groupId, regex, email, sortBy, direction, page, size);
    }


    @GetMapping("inbox/threads")
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    public Mono<Page<ThreadDTO>> getInboxPage(@RequestParam String groupId,
                                              @RequestParam(defaultValue = "") String regex,
                                              @RequestParam(defaultValue = "") String email,
                                              @RequestParam(defaultValue = "created") String sortBy,
                                              @RequestParam(defaultValue = "DESC") Sort.Direction direction,
                                              @RequestParam(defaultValue = "0") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInbox(groupId, regex, email, sortBy, direction, page, size)
                .collectList()
                .zipWith(super.countInbox(groupId, regex, email))
                .map(p -> new PageImpl<>(p.getT1(), PageRequest.of(page, size, Sort.by(direction, sortBy)), p.getT2()));
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groups)")
    public Flux<ThreadDTO> searchInboxUnread(List<String> groups, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInboxUnread(groups, email, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    public Flux<ThreadDTO> searchOutbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        return super.searchOutbox(groupId, regex, email, sortBy, direction, page, size);
    }

    @GetMapping("outbox/threads")
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    public Mono<Page<ThreadDTO>> getOutboxPage(@RequestParam String groupId,
                                               @RequestParam(defaultValue = "") String regex,
                                               @RequestParam(defaultValue = "created") String sortBy,
                                               @RequestParam(defaultValue = "DESC") Sort.Direction direction,
                                               @RequestParam(defaultValue = "0") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        String email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchOutbox(groupId, regex, email, sortBy, direction, page, size)
                .collectList()
                .zipWith(super.countOutbox(groupId, regex, email))
                .map(p -> new PageImpl<>(p.getT1(), PageRequest.of(page, size, Sort.by(direction, sortBy)), p.getT2()));
    }

    @Override
    @PreAuthorize("isAuthenticated() and authentication.principal.getAttribute('email') == #message.from.email")
    public Mono<ThreadDTO> addMessage(String threadId, Message message, boolean anonymous) {
        return super.addMessage(threadId, message, anonymous).doOnNext(t ->
                Mono.fromRunnable(() -> {
                            messagingService.updateUnread(t);
                            emailOperations.sendEmails(t);
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
        );
    }

    @Override
    @PreAuthorize("isFullyAuthenticated()")
    public Mono<ThreadDTO> readMessage(String threadId, String messageId, boolean read, String userId) {
        userId = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.readMessage(threadId, messageId, read, userId);
    }

    public Mono<UnreadThreads> getUnread(String email) {
        return getUnread(new ArrayList<>(), email);
    }

    public Mono<UnreadThreads> getUnread(List<String> groups, String email) {
        groups = messagingService.filterUserGroups(email, groups);
        return super.searchUnreadThreads(groups, email);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ServerError> accessDenied(HttpServletRequest req, Exception ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        return ResponseEntity
                .status(status)
                .body(new ServerError(status, req, ex));
    }
}
