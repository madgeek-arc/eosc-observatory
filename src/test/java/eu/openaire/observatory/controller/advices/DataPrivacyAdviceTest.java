package eu.openaire.observatory.controller.advices;

import eu.openaire.observatory.configuration.PrivacyProperties;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.service.SecurityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;

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

    private MethodParameter methodParameter(String methodName) throws NoSuchMethodException {
        Method method = TestController.class.getDeclaredMethod(methodName);
        return new MethodParameter(method, -1);
    }

    public static class TestController {
        public PrivatePayload singlePayload() {
            return null;
        }

        public ResponseEntity<PayloadEnvelope<PrivatePayload>> wrappedPayload() {
            return null;
        }
    }

    public static class PayloadEnvelope<T> {
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
}
