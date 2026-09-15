package eu.openaire.observatory.configuration.logging;

import eu.openaire.observatory.service.ErasureSubjectReference;
import eu.openaire.observatory.utils.UserIds;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.StringMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ErasureMaskingRewritePolicyTest {

    private static final String SECRET = "rewrite-policy-test-secret";
    private static final String ERASED_USER = "alice@example.org";

    private final ErasureMaskingRewritePolicy policy = ErasureMaskingRewritePolicy.createPolicy();

    private static String refFor(String userId) {
        return ErasureSubjectReference.hmacHex(SECRET, UserIds.normalize(userId));
    }

    @BeforeEach
    void configure() {
        ErasureLogMasking.configure(SECRET, List.of(refFor(ERASED_USER)));
    }

    @AfterEach
    void reset() {
        ErasureLogMasking.reset();
    }

    private static LogEvent eventWithUser(String user) {
        StringMap contextData = ContextDataFactory.createContextData();
        if (user != null) {
            contextData.putValue("user", user);
        }
        return Log4jLogEvent.newBuilder()
                .setLoggerName("test")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("event"))
                .setContextData(contextData)
                .build();
    }

    @Test
    void masksTheUserFieldForAnErasedSubject() {
        LogEvent rewritten = policy.rewrite(eventWithUser(ERASED_USER));

        assertEquals("erased:" + refFor(ERASED_USER), rewritten.getContextData().getValue("user"));
    }

    @Test
    void leavesALivingUserEventUntouched() {
        LogEvent original = eventWithUser("bob@example.org");

        LogEvent rewritten = policy.rewrite(original);

        assertSame(original, rewritten);
        assertEquals("bob@example.org", rewritten.getContextData().getValue("user"));
    }

    @Test
    void leavesAnEventWithNoUserFieldUntouched() {
        LogEvent original = eventWithUser(null);

        assertSame(original, policy.rewrite(original));
    }
}
