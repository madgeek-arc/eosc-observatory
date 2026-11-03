package eu.openaire.observatory.utils;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserIdsTest {

    private static final String CANONICAL = "jane.doe@example.org";

    @Test
    void normalizeReturnsNullForNull() {
        assertNull(UserIds.normalize(null));
    }

    @Test
    void normalizeLeavesCanonicalIdUnchanged() {
        assertEquals(CANONICAL, UserIds.normalize(CANONICAL));
    }

    @Test
    void normalizeLowercases() {
        assertEquals(CANONICAL, UserIds.normalize("Jane.Doe@Example.ORG"));
    }

    @Test
    void normalizeTrimsSurroundingWhitespace() {
        assertEquals(CANONICAL, UserIds.normalize("  " + CANONICAL + "\t\n"));
    }

    /**
     * The regression this utility exists for: two stakeholder version rows held the address with its
     * quotes included and survived a completed purge, because {@code "\"x@y\"" != "x@y"}.
     */
    @Test
    void normalizeStripsSurroundingQuotes() {
        assertEquals(CANONICAL, UserIds.normalize("\"" + CANONICAL + "\""));
    }

    @Test
    void normalizeHandlesWhitespaceInsideAndOutsideQuotes() {
        assertEquals(CANONICAL, UserIds.normalize("  \" " + CANONICAL.toUpperCase(Locale.ROOT) + " \"  "));
    }

    @Test
    void normalizeLeavesUnbalancedQuotesAlone() {
        assertEquals("\"" + CANONICAL, UserIds.normalize("\"" + CANONICAL));
        assertEquals(CANONICAL + "\"", UserIds.normalize(CANONICAL + "\""));
    }

    @Test
    void normalizeDoesNotEmptyALoneQuoteCharacter() {
        assertEquals("\"", UserIds.normalize("\""));
    }

    @Test
    void normalizeReturnsEmptyStringForBlankInput() {
        assertEquals("", UserIds.normalize("   "));
    }

    @Test
    void normalizeIsIdempotent() {
        String once = UserIds.normalize("  \"Jane.Doe@Example.ORG\"  ");
        assertEquals(once, UserIds.normalize(once));
        assertEquals(CANONICAL, once);
    }

    /**
     * Guards the {@link Locale#ROOT} argument: the default-locale {@code toLowerCase()} maps
     * {@code I} to a dotless {@code ı} under a Turkish locale, which would make an id's canonical
     * form depend on where the server happens to run.
     */
    @Test
    void normalizeIsIndependentOfTheDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals("doe@example.org", UserIds.normalize("DOE@EXAMPLE.ORG"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
