package eu.openaire.observatory.configuration.logging;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.erasure.domain.ErasureRecord;
import eu.openaire.observatory.erasure.repository.ErasureRecordRepository;
import eu.openaire.observatory.service.ErasureSubjectReference;
import eu.openaire.observatory.utils.UserIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class ErasureLogMaskingTest {

    private static final String SECRET = "log-masking-test-secret";

    @AfterEach
    void reset() {
        ErasureLogMasking.reset();
    }

    private static String refFor(String userId) {
        return ErasureSubjectReference.hmacHex(SECRET, UserIds.normalize(userId));
    }

    @Test
    void startupMasksOnlySuccessfulErasureSubjects() {
        var properties = new ApplicationProperties();
        properties.setHmacSecret(SECRET);
        var repository = mock(
                ErasureRecordRepository.class);
        var successful = new ErasureRecord()
                .setSubjectRef(refFor("alice@example.org")).setOutcome("SUCCESS");
        var pending = new ErasureRecord()
                .setSubjectRef(refFor("bob@example.org")).setOutcome("PENDING");
        lenient().when(repository.findAll()).thenReturn(List.of(successful, pending));
        when(repository.findAllByOutcome("SUCCESS")).thenReturn(List.of(successful));

        new ErasureLogMaskingConfig(properties, repository).primeLogMasking();

        assertEquals("erased:" + successful.getSubjectRef(),
                ErasureLogMasking.maskedReferenceFor("alice@example.org"));
        assertNull(ErasureLogMasking.maskedReferenceFor("bob@example.org"));
        verify(repository).findAllByOutcome("SUCCESS");
    }

    @Test
    void returnsNullBeforeConfigured() {
        assertNull(ErasureLogMasking.maskedReferenceFor("alice@example.org"));
    }

    @Test
    void masksAKnownErasedSubject() {
        String ref = refFor("alice@example.org");
        ErasureLogMasking.configure(SECRET, List.of(ref));

        assertEquals("erased:" + ref, ErasureLogMasking.maskedReferenceFor("alice@example.org"));
    }

    @Test
    void matchesRegardlessOfCasingOrQuoteWrapping() {
        String ref = refFor("alice@example.org");
        ErasureLogMasking.configure(SECRET, List.of(ref));

        assertEquals("erased:" + ref, ErasureLogMasking.maskedReferenceFor("\"Alice@Example.ORG\""));
    }

    @Test
    void leavesALivingUserUntouched() {
        ErasureLogMasking.configure(SECRET, List.of(refFor("alice@example.org")));

        assertNull(ErasureLogMasking.maskedReferenceFor("bob@example.org"));
    }

    @Test
    void picksUpSubjectsRegisteredAfterStartup() {
        ErasureLogMasking.configure(SECRET, List.of());
        String ref = refFor("carol@example.org");

        ErasureLogMasking.registerErased(ref);

        assertEquals("erased:" + ref, ErasureLogMasking.maskedReferenceFor("carol@example.org"));
    }

    @Test
    void blankSecretDisablesMasking() {
        ErasureLogMasking.configure("", List.of(refFor("alice@example.org")));

        assertNull(ErasureLogMasking.maskedReferenceFor("alice@example.org"));
    }

    @Test
    void ignoresEmptyUserValues() {
        ErasureLogMasking.configure(SECRET, List.of(refFor("alice@example.org")));

        assertNull(ErasureLogMasking.maskedReferenceFor(null));
        assertNull(ErasureLogMasking.maskedReferenceFor("  "));
    }
}
