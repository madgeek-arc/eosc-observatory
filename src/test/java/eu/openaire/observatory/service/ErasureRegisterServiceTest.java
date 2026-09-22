package eu.openaire.observatory.service;

import eu.openaire.observatory.erasure.domain.ErasureRecord;
import eu.openaire.observatory.erasure.repository.ErasureRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErasureRegisterServiceTest {
    @Mock private ErasureRecordRepository repository;
    @InjectMocks private ErasureRegisterService service;

    @Test
    void beginRecordsPendingWithNoCompletionTime() {
        ErasureRecord record = new ErasureRecord().setSubjectRef("hmac").setOutcome("SUCCESS")
                .setStartedAt(Instant.EPOCH).setCompletedAt(Instant.now());
        assertEquals(record.getAttemptId(), service.begin(record));
        assertEquals("PENDING", record.getOutcome());
        assertEquals(Instant.EPOCH, record.getStartedAt());
        assertNull(record.getCompletedAt());
        var ordered = inOrder(repository);
        ordered.verify(repository).lockSubject("hmac");
        ordered.verify(repository).findBySubjectRefAndOutcome("hmac", "PENDING");
        ordered.verify(repository).save(record);
    }

    @Test
    void retryReturnsOriginalAttemptWithoutChangingAuditFields() {
        ErasureRecord original = new ErasureRecord().setOutcome("PENDING").setSurveyAnswers(4)
                .setStartedAt(Instant.EPOCH).setExecutedBy("original-operator");
        when(repository.findBySubjectRefAndOutcome("hmac", "PENDING")).thenReturn(Optional.of(original));
        assertEquals(original.getAttemptId(), service.begin(new ErasureRecord().setSubjectRef("hmac")
                .setSurveyAnswers(0).setExecutedBy("retry-operator")));
        verify(repository, never()).save(any());
        assertTrue(service.isPending("hmac"));
        assertEquals(4, original.getSurveyAnswers());
        assertEquals(Instant.EPOCH, original.getStartedAt());
        assertEquals("original-operator", original.getExecutedBy());
    }

    @Test
    void laterErasureCreatesAnotherAttemptWithoutOverwritingCompletedEvidence() {
        ErasureRecord first = new ErasureRecord().setSubjectRef("hmac").setSurveyAnswers(4)
                .setStartedAt(Instant.EPOCH).setCompletedAt(Instant.EPOCH).setOutcome("SUCCESS");
        ErasureRecord next = new ErasureRecord().setSubjectRef("hmac").setSurveyAnswers(2);
        // Only pending attempts are reused; a previous success does not satisfy that query.
        assertEquals(next.getAttemptId(), service.begin(next));
        assertNotEquals(first.getAttemptId(), next.getAttemptId());
        verify(repository).findBySubjectRefAndOutcome("hmac", "PENDING");
        verify(repository).save(next);
        verify(repository, never()).save(first);
        assertEquals("SUCCESS", first.getOutcome());
        assertEquals(Instant.EPOCH, first.getCompletedAt());
        assertEquals(4, first.getSurveyAnswers());
    }

    @Test
    void completeChangesOnlyLifecycleFieldsOfTheSpecifiedAttempt() {
        ErasureRecord original = new ErasureRecord().setSubjectRef("hmac").setOutcome("PENDING")
                .setStartedAt(Instant.EPOCH).setSurveyAnswers(4).setExecutedBy("operator");
        when(repository.findById(original.getAttemptId())).thenReturn(Optional.of(original));
        service.complete(original.getAttemptId());
        assertEquals("SUCCESS", original.getOutcome());
        assertTrue(original.getCompletedAt().isAfter(Instant.EPOCH));
        assertEquals(Instant.EPOCH, original.getStartedAt());
        assertEquals(4, original.getSurveyAnswers());
        assertEquals("operator", original.getExecutedBy());
        verify(repository).save(original);
    }

    @Test
    void repeatingCompletionPreservesItsOriginalTimestamp() {
        ErasureRecord completed = new ErasureRecord().setOutcome("SUCCESS").setCompletedAt(Instant.EPOCH);
        when(repository.findById(completed.getAttemptId())).thenReturn(Optional.of(completed));
        service.complete(completed.getAttemptId());
        assertEquals(Instant.EPOCH, completed.getCompletedAt());
        verify(repository, never()).save(any());
    }

    @Test
    void missingRecordIsNotPendingAndCannotBeCompleted() {
        assertFalse(service.isPending("hmac"));
        assertThrows(java.util.NoSuchElementException.class, () -> service.complete(UUID.randomUUID()));
        verify(repository, never()).save(any());
    }
}
