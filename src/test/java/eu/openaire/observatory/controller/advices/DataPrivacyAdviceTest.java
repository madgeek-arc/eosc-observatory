package eu.openaire.observatory.controller.advices;

import eu.openaire.observatory.configuration.PrivacyProperties;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.service.SecurityService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataPrivacyAdviceTest {

    @Mock
    private SecurityService securityService;
    @Mock
    private MethodSecurityExpressions securityExpressions;

    private DataPrivacyAdvice<Object> advice;

    @BeforeEach
    void setUp() {
        PrivacyProperties privacyProperties = new PrivacyProperties();

        PrivacyProperties.FieldPrivacy fieldPrivacy = new PrivacyProperties.FieldPrivacy();
        fieldPrivacy.setClassName(PrivatePayload.class.getCanonicalName());
        fieldPrivacy.setField("secret");
        privacyProperties.getEntries().add(fieldPrivacy);

        PrivacyProperties.WrapperClass wrapperClass = new PrivacyProperties.WrapperClass();
        wrapperClass.setClazz(PayloadEnvelope.class);
        wrapperClass.setField("content");
        privacyProperties.getWrapperClasses().add(wrapperClass);

        advice = new DataPrivacyAdvice<>(securityService, securityExpressions, privacyProperties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void beforeBodyWriteReturnsSanitizedCopyForAnonymousUser() throws NoSuchMethodException {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        ));
        PrivatePayload body = new PrivatePayload("doc-1", "visible");

        Object result = advice.beforeBodyWrite(
                body,
                methodParameter("singlePayload"),
                null,
                null,
                null,
                null
        );

        PrivatePayload sanitized = (PrivatePayload) result;
        assertNotSame(body, sanitized);
        assertEquals("visible", body.getSecret());
        assertNull(sanitized.getSecret());
        verifyNoInteractions(securityService, securityExpressions);
    }

    @Test
    void getPathIncludesConfiguredWrapperField() throws NoSuchMethodException {
        String path = advice.getPath("secret", methodParameter("wrappedPayload"));

        assertEquals("content.secret", path);
    }

    @Test
    void replaceDataClearsNestedCollectionField() throws Exception {
        PayloadCollectionWrapper wrapper = new PayloadCollectionWrapper(List.of(
                new PrivatePayload("doc-1", "first"),
                new PrivatePayload("doc-2", "second")
        ));

        advice.replaceData(wrapper, "items[].secret", null);

        assertNull(wrapper.getItems().get(0).getSecret());
        assertNull(wrapper.getItems().get(1).getSecret());
    }

    @Test
    void collectionBranch_sanitizesDataForAnonymousUser() throws NoSuchMethodException {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        List<PrivatePayload> body = new ArrayList<>();
        body.add(new PrivatePayload("doc-1", "secret-value"));

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> result = (List<java.util.Map<String, Object>>)
                advice.beforeBodyWrite(body, methodParameter("listPayload"), null, null, null, null);

        assertNotNull(result);
        assertNull(result.get(0).get("secret"),
                "Sensitive field must be null for anonymous user in collection response");
    }

    @Test
    @SuppressWarnings("unchecked")
    void collectionBranch_preservesDataForAdmin() throws NoSuchMethodException {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null, AuthorityUtils.createAuthorityList("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        lenient().when(securityExpressions.isAdmin(adminAuth)).thenReturn(true);

        List<PrivatePayload> body = new ArrayList<>();
        body.add(new PrivatePayload("doc-1", "secret-value"));

        List<PrivatePayload> result = (List<PrivatePayload>)
                advice.beforeBodyWrite(body, methodParameter("listPayload"), null, null, null, null);

        assertEquals("secret-value", result.get(0).getSecret(),
                "Admin must receive unscrubbed data");
    }

    @Test
    void beforeBodyWrite_failsClosedOnDeserializationError() throws NoSuchMethodException {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        DataPrivacyAdvice<Object> adviceForNoArgCtor = buildAdviceFor(NoArgConstructorPayload.class, "secret");
        NoArgConstructorPayload body = NoArgConstructorPayload.of("doc-1", "sensitive-data");

        assertThrows(ResourceException.class,
                () -> adviceForNoArgCtor.beforeBodyWrite(
                        body, methodParameter("noArgCtorPayload"), null, null, null, null),
                "Must not silently return unredacted body when deserialization fails");
    }

    @Test
    void unwrapResponseClass_resolvesCorrectly_whenMatchingWrapperIsNotFirst() throws NoSuchMethodException {
        PrivacyProperties multiWrapperProps = buildPropsWithTwoWrappers();
        DataPrivacyAdvice<Object> multiAdvice =
                new DataPrivacyAdvice<>(securityService, securityExpressions, multiWrapperProps);

        Class<?> resolved = multiAdvice.unwrapResponseClass(methodParameter("secondWrappedPayload"));

        assertEquals(PrivatePayload.class, resolved);
    }

    /**
     * Verifies that getPath() completes when wrapperClasses is empty. The method uses a bounded
     * loop; if unbounded it would hang forever on any type that is not ResponseEntity or Collection.
     * The test spawns getPath in a separate thread — the thread is leaked if it hangs but has no
     * side effects and terminates when the JVM exits.
     */
    @Test
    void getPath_completesWithEmptyWrapperClasses() throws Exception {
        PrivacyProperties emptyWrapperProps = new PrivacyProperties();
        PrivacyProperties.FieldPrivacy fp = new PrivacyProperties.FieldPrivacy();
        fp.setClassName(PrivatePayload.class.getCanonicalName());
        fp.setField("secret");
        emptyWrapperProps.getEntries().add(fp);

        DataPrivacyAdvice<Object> adviceNoWrappers =
                new DataPrivacyAdvice<>(securityService, securityExpressions, emptyWrapperProps);
        MethodParameter mp = methodParameter("singlePayload");

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<String> future = exec.submit(() -> adviceNoWrappers.getPath("secret", mp));
        exec.shutdown();

        try {
            String path = future.get(1, TimeUnit.SECONDS);
            assertEquals("secret", path);
        } catch (TimeoutException e) {
            future.cancel(true);
            fail("getPath() hung — loop has no iteration cap when wrapperClasses is empty");
        }
    }

    @Test
    void getId_throwsForObjectWithNoIdField() {
        assertThrows(RuntimeException.class,
                () -> advice.getId(new NoIdPayload("some-name")),
                "getId() must throw when the source object has no 'id' field");
    }

    @Test
    void beforeBodyWrite_handlesNullAuthGracefully() throws NoSuchMethodException {
        SecurityContextHolder.clearContext(); // authentication == null
        PrivatePayload body = new PrivatePayload("doc-1", "secret-value");

        // Simulate what the real SecurityService does with null auth.
        // Lenient: if the fix guards null auth before reaching canRead the stubs are unused.
        lenient().when(securityExpressions.isAdmin(null)).thenReturn(false);
        lenient().when(securityService.canRead(isNull(), eq("doc-1")))
                .thenThrow(new InsufficientAuthenticationException("null auth"));

        assertDoesNotThrow(
                () -> advice.beforeBodyWrite(body, methodParameter("singlePayload"), null, null, null, null),
                "Null Authentication must not propagate as InsufficientAuthenticationException");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MethodParameter methodParameter(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    private DataPrivacyAdvice<Object> buildAdviceFor(Class<?> privacyClass, String privacyField) {
        PrivacyProperties props = new PrivacyProperties();
        PrivacyProperties.FieldPrivacy fp = new PrivacyProperties.FieldPrivacy();
        fp.setClassName(privacyClass.getCanonicalName());
        fp.setField(privacyField);
        props.getEntries().add(fp);
        return new DataPrivacyAdvice<>(securityService, securityExpressions, props);
    }

    private PrivacyProperties buildPropsWithTwoWrappers() {
        PrivacyProperties props = new PrivacyProperties();

        PrivacyProperties.FieldPrivacy fp = new PrivacyProperties.FieldPrivacy();
        fp.setClassName(PrivatePayload.class.getCanonicalName());
        fp.setField("secret");
        props.getEntries().add(fp);

        PrivacyProperties.WrapperClass first = new PrivacyProperties.WrapperClass();
        first.setClazz(PayloadEnvelope.class);
        first.setField("content");
        props.getWrapperClasses().add(first);

        PrivacyProperties.WrapperClass second = new PrivacyProperties.WrapperClass();
        second.setClazz(SecondWrapper.class);
        second.setField("content");
        props.getWrapperClasses().add(second);

        return props;
    }

    // -------------------------------------------------------------------------
    // Test controller — return-type declarations used to build MethodParameter
    // -------------------------------------------------------------------------

    public static class TestController {
        public PrivatePayload singlePayload() {
            return null;
        }

        public ResponseEntity<PayloadEnvelope<PrivatePayload>> wrappedPayload() {
            return null;
        }

        public List<PrivatePayload> listPayload() {
            return null;
        }

        public SecondWrapper<PrivatePayload> secondWrappedPayload() {
            return null;
        }

        public NoArgConstructorPayload noArgCtorPayload() {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Domain fixtures
    // -------------------------------------------------------------------------

    public static class PayloadEnvelope<T> {
        private T content;

        public T getContent() {
            return content;
        }

        public void setContent(T content) {
            this.content = content;
        }
    }

    public static class SecondWrapper<T> {
        private T content;

        public T getContent() {
            return content;
        }

        public void setContent(T content) {
            this.content = content;
        }
    }

    public static class PayloadCollectionWrapper {
        private List<PrivatePayload> items;

        PayloadCollectionWrapper(List<PrivatePayload> items) {
            this.items = items;
        }

        public List<PrivatePayload> getItems() {
            return items;
        }

        public void setItems(List<PrivatePayload> items) {
            this.items = items;
        }
    }

    public static class PrivatePayload {
        private String id;
        private String secret;

        PrivatePayload() {
        }

        PrivatePayload(String id, String secret) {
            this.id = id;
            this.secret = secret;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    /** No no-arg constructor — triggers InvalidDefinitionException on Jackson readValue. */
    public static class NoArgConstructorPayload {
        private final String id;
        private final String secret;

        private NoArgConstructorPayload(String id, String secret) {
            this.id = id;
            this.secret = secret;
        }

        public static NoArgConstructorPayload of(String id, String secret) {
            return new NoArgConstructorPayload(id, secret);
        }

        public String getId() {
            return id;
        }

        public String getSecret() {
            return secret;
        }
    }

    /** No "id" field — convertValue produces an IdField with a null id. */
    public static class NoIdPayload {
        private final String name;

        NoIdPayload(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
