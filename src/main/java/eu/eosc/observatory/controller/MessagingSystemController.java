package eu.eosc.observatory.controller;

import gr.athenarc.messaging.config.MessagingClientProperties;
import gr.athenarc.messaging.controller.MessagingController;
import gr.athenarc.messaging.controller.RestApiPaths;
import gr.athenarc.messaging.domain.Message;
import gr.athenarc.messaging.domain.TopicThread;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.dto.UnreadMessages;
import gr.athenarc.recaptcha.annotation.ReCaptcha;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class MessagingSystemController extends MessagingController {

    public MessagingSystemController(MessagingClientProperties messagingClientProperties) {
        super(messagingClientProperties);
    }

    @ReCaptcha("#recaptcha")
    @PostMapping(RestApiPaths.THREADS + "/public")
    public Mono<ThreadDTO> addExternal(@RequestHeader("g-recaptcha-response") String recaptcha, @RequestBody ThreadDTO thread) {
        return super.add(thread);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME:
    public Mono<ThreadDTO> get(String threadId) {
        return super.get(threadId);
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
    public Mono<UnreadMessages> searchTotalUnread(List<String> groups) {
        return super.searchTotalUnread(groups); // 1 FIXME: returns 0 and empty group list
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME: check if user is member of provided groupId
    public Flux<ThreadDTO> searchInbox(String groupId, String regex, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        return super.searchInbox(groupId, regex, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME: check if user is member of provided groups
    // TODO: remove groups argument and find it by authenticated user
    public Flux<ThreadDTO> searchInboxUnread(List<String> groups, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        return super.searchInboxUnread(groups, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    // FIXME: check if user is member of provided groupId
    // TODO: check if email is matching with user emails
    public Flux<ThreadDTO> searchOutbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        return super.searchOutbox(groupId, regex, email, sortBy, direction, page, size);
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
    public Mono<ThreadDTO> readMessage(String threadId, String messageId, boolean read) {
        return super.readMessage(threadId, messageId, read);
    }
}
