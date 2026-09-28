package eu.openaire.observatory.service;

import eu.openaire.observatory.commenting.CommentNotificationService;
import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.commenting.repository.CommentMessageRepository;
import eu.openaire.observatory.commenting.repository.CommentRepository;
import eu.openaire.observatory.mappers.CommentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyAnswerCommentServiceTest {

    private static final String USER_ID = "user@example.org";
    private static final String PLACEHOLDER = "[Deleted User]";

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentMessageRepository messageRepository;
    @Mock
    private CommentMapper mapper;
    @Mock
    private CommentNotificationService notificationService;

    private SurveyAnswerCommentService service;

    @BeforeEach
    void setUp() {
        service = new SurveyAnswerCommentService(commentRepository, messageRepository, mapper, notificationService);
    }

    @Test
    void anonymizeUserScrubsMentionMarkupCaseInsensitively() {
        CommentMessage message = messageWithBody("Hey @{John Doe}(User@Example.ORG) check this out");
        when(messageRepository.findMessagesMentioning(USER_ID)).thenReturn(List.of(message));

        service.anonymizeUser(USER_ID, PLACEHOLDER);

        assertEquals("Hey " + PLACEHOLDER + " check this out", message.getBody());
        verify(messageRepository).save(message);
    }

    @Test
    void anonymizeUserScrubsMultipleMentionsOfSameUserInOneBody() {
        CommentMessage message = messageWithBody("@{A}(user@example.org) and again @{A}(user@example.org)");
        when(messageRepository.findMessagesMentioning(USER_ID)).thenReturn(List.of(message));

        service.anonymizeUser(USER_ID, PLACEHOLDER);

        assertEquals(PLACEHOLDER + " and again " + PLACEHOLDER, message.getBody());
        verify(messageRepository).save(message);
    }

    @Test
    void anonymizeUserLeavesBodyUntouchedWhenMarkupDoesNotMatch() {
        String body = "No mention markup for this user here";
        CommentMessage message = messageWithBody(body);
        when(messageRepository.findMessagesMentioning(USER_ID)).thenReturn(List.of(message));

        service.anonymizeUser(USER_ID, PLACEHOLDER);

        assertEquals(body, message.getBody());
        verify(messageRepository, never()).save(message);
    }

    @Test
    void anonymizeUserDeletesRedundantMentionsBeforeAnonymizingToAvoidPkCollision() {
        when(messageRepository.findMessagesMentioning(USER_ID)).thenReturn(List.of());

        service.anonymizeUser(USER_ID, PLACEHOLDER);

        verify(messageRepository).anonymizeAuthor(USER_ID, PLACEHOLDER);
        InOrder order = inOrder(messageRepository);
        order.verify(messageRepository).deleteRedundantMentions(USER_ID, PLACEHOLDER);
        order.verify(messageRepository).anonymizeMentions(USER_ID, PLACEHOLDER);
    }

    private CommentMessage messageWithBody(String body) {
        CommentMessage message = new CommentMessage();
        message.setBody(body);
        return message;
    }
}
