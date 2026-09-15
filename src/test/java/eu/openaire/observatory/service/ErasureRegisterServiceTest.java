package eu.openaire.observatory.service;

import eu.openaire.observatory.commenting.domain.ErasureRecord;
import eu.openaire.observatory.commenting.repository.ErasureRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErasureRegisterServiceTest {

    @Mock
    private ErasureRecordRepository repository;

    @InjectMocks
    private ErasureRegisterService service;

    @Test
    void recordWritesWhenSubjectIsNew() {
        when(repository.existsById("hmac-1")).thenReturn(false);

        boolean written = service.record(new ErasureRecord().setSubjectRef("hmac-1").setOutcome("SUCCESS"));

        assertTrue(written);
        verify(repository).save(any(ErasureRecord.class));
    }

    @Test
    void recordIsANoOpWhenSubjectAlreadyRecorded() {
        when(repository.existsById("hmac-1")).thenReturn(true);

        boolean written = service.record(new ErasureRecord().setSubjectRef("hmac-1").setOutcome("SUCCESS"));

        assertFalse(written);
        verify(repository, never()).save(any());
    }
}
