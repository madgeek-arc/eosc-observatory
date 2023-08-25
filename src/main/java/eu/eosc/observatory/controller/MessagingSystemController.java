package eu.eosc.observatory.controller;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserInfo;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.UserService;
import eu.eosc.observatory.sse.Emitter;
import eu.eosc.observatory.sse.EmitterMessage;
import eu.eosc.observatory.sse.EmitterService;
import gr.athenarc.messaging.config.MessagingClientProperties;
import gr.athenarc.messaging.controller.MessagingController;
import gr.athenarc.messaging.controller.RestApiPaths;
import gr.athenarc.messaging.domain.Correspondent;
import gr.athenarc.messaging.domain.Message;
import gr.athenarc.messaging.domain.TopicThread;
import gr.athenarc.messaging.dto.MessageDTO;
import gr.athenarc.messaging.dto.ThreadDTO;
import gr.athenarc.messaging.dto.UnreadThreads;
import gr.athenarc.messaging.mailer.client.service.MailClient;
import gr.athenarc.messaging.mailer.domain.EmailMessage;
import gr.athenarc.recaptcha.annotation.ReCaptcha;
import org.springframework.data.domain.Sort;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@RestController
public class MessagingSystemController extends MessagingController {

    private final UserService userService;
    private final CoordinatorService coordinatorService;
    private final StakeholderService stakeholderService;
    private final EmitterService emitterService;
    private final MailClient mailClient;

    public MessagingSystemController(MessagingClientProperties messagingClientProperties,
                                     UserService userService,
                                     CoordinatorService coordinatorService,
                                     StakeholderService stakeholderService,
                                     EmitterService emitterService,
                                     MailClient mailClient) {
        super(messagingClientProperties);
        this.userService = userService;
        this.coordinatorService = coordinatorService;
        this.stakeholderService = stakeholderService;
        this.emitterService = emitterService;
        this.mailClient = mailClient;
    }

    @ReCaptcha("#recaptcha")
    @PostMapping(RestApiPaths.THREADS + "/public")
    public Mono<ThreadDTO> addExternal(@RequestHeader("g-recaptcha-response") String recaptcha, @RequestBody ThreadDTO thread) {
        return super.add(thread).doOnSuccess(t ->
                Mono.fromRunnable(() -> sendEmails(t))
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
        return super.add(thread).doOnSuccess(t ->
                Mono.fromRunnable(() -> sendEmails(t))
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

    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groups)")
    @GetMapping("/stream-2/inbox/unread")
    public SseEmitter streamSseMvc(@RequestParam(defaultValue = "") List<String> groups, @RequestParam String email) {
        Emitter emitter = emitterService.getUserEmitter(User.getId(SecurityContextHolder.getContext().getAuthentication()));
        SseEmitter sseEmitter = new SseEmitter(Long.MAX_VALUE);
        emitter.addSseEmitter(sseEmitter);
        return sseEmitter;
    }

    public UnreadThreads getUnread(List<String> groups, String email) {
        groups = filterUserGroups(email, groups);
        return super.searchUnreadThreads(groups, email).block();
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groups)")
    public Flux<ServerSentEvent<UnreadThreads>> streamUnreadThreads(@RequestParam(defaultValue = "") List<String> groups, @RequestParam String email) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        groups = filterUserGroups(email, groups);
        return super.streamUnreadThreads(groups, email);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groups)")
    public Mono<UnreadThreads> searchUnreadThreads(@RequestParam(defaultValue = "") List<String> groups, String email) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        groups = filterUserGroups(email, groups);
        return super.searchUnreadThreads(groups, email);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    public Flux<ThreadDTO> searchInbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInbox(groupId, regex, email, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groups)")
    // TODO: remove groups argument and find it by authenticated user
    public Flux<ThreadDTO> searchInboxUnread(List<String> groups, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        email = User.getId(SecurityContextHolder.getContext().getAuthentication());
        return super.searchInboxUnread(groups, email, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated() and @methodSecurityExpressionsService.userIsMemberOfGroup(authentication.principal.getAttribute('email'), #groupId)")
    // TODO: check if email is matching with user emails
    public Flux<ThreadDTO> searchOutbox(String groupId, String regex, String email, String sortBy, Sort.Direction direction, Integer page, Integer size) {
        return super.searchOutbox(groupId, regex, email, sortBy, direction, page, size);
    }

    @Override
    @PreAuthorize("isAuthenticated() and authentication.principal.getAttribute('email') == #message.from.email")
    public Mono<ThreadDTO> addMessage(String threadId, Message message, boolean anonymous) {
        return super.addMessage(threadId, message, anonymous).doOnSuccess(t ->
                Mono.fromRunnable(() -> sendEmails(t))
                        .subscribeOn(Schedulers.boundedElastic())
                        .subscribe()
        );
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

    private List<String> filterUserGroups(String email, List<String> groups) {
        List<String> userGroups = new ArrayList<>();
        UserInfo info = userService.getUserInfo(email);
        userGroups.addAll(info.getStakeholders().stream().map(Stakeholder::getId).collect(Collectors.toList()));
        userGroups.addAll(info.getCoordinators().stream().map(Coordinator::getId).collect(Collectors.toList()));
        if (!groups.isEmpty()) {
            userGroups = userGroups.stream().filter(groups::contains).collect(Collectors.toList());
        }
        return userGroups;
    }

    List<String> getEmailsOfGroup(String groupId) {
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

    void sendEmails(ThreadDTO thread) {
        MessageDTO messageDTO = thread.getMessages().get(thread.getMessages().size() - 1);
        List<String> ccEmails = new ArrayList<>();
        List<String> bccEmails = new ArrayList<>();
        bccEmails.addAll(messageDTO.getTo().stream().map(Correspondent::getGroupId).map(this::getEmailsOfGroup).flatMap(Collection::stream).collect(Collectors.toList()));
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
