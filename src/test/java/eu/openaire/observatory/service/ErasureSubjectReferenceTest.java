package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import gr.uoa.di.madgik.registry.service.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErasureSubjectReferenceTest {

    private static final String USER_ID = "jane.doe@example.org";

    private ApplicationProperties properties;
    private ErasureSubjectReference reference;

    @BeforeEach
    void setUp() {
        properties = new ApplicationProperties();
        properties.setErasureHashSecret("test-erasure-secret");
        reference = new ErasureSubjectReference(properties);
    }

    @Test
    void referenceIsHexEncodedSha256Length() {
        String ref = reference.of(USER_ID);

        assertEquals(64, ref.length());
        assertTrue(ref.matches("[0-9a-f]{64}"), "expected lowercase hex, got: " + ref);
    }

    @Test
    void sameIdAlwaysYieldsTheSameReference() {
        assertEquals(reference.of(USER_ID), reference.of(USER_ID));
    }

    /**
     * The reference is computed over the <em>normalized</em> id, so casing and the quote-wrapping
     * seen in stored ids cannot produce two different references for one person — and re-running
     * purge() on an already-purged id logs an identical reference.
     */
    @Test
    void referenceIsStableAcrossCasingAndQuotingVariants() {
        String canonical = reference.of(USER_ID);

        assertEquals(canonical, reference.of("Jane.Doe@Example.ORG"));
        assertEquals(canonical, reference.of("\"" + USER_ID + "\""));
        assertEquals(canonical, reference.of("  \"JANE.DOE@EXAMPLE.ORG\"  "));
    }

    @Test
    void differentIdsYieldDifferentReferences() {
        assertNotEquals(reference.of(USER_ID), reference.of("someone.else@example.org"));
    }

    /**
     * A keyed HMAC, not a bare digest: emails are low-entropy, so an unsalted hash would fall to a
     * dictionary attack. Changing the key must change every reference.
     */
    @Test
    void referenceDependsOnTheSecret() {
        String withFirstSecret = reference.of(USER_ID);

        properties.setErasureHashSecret("a-different-secret");

        assertNotEquals(withFirstSecret, reference.of(USER_ID));
    }

    @Test
    void missingSecretFailsLoudlyRatherThanRecordingAnUnkeyedHash() {
        properties.setErasureHashSecret("  ");

        assertThrows(ServiceException.class, () -> reference.of(USER_ID));
    }
}
