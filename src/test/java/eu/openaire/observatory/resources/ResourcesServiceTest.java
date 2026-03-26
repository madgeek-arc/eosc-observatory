package eu.openaire.observatory.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.resources.model.DocumentMetadata;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourcesServiceTest {

    @Mock
    private GenericResourceService genericResourceService;

    private ResourcesService service;

    @BeforeEach
    void setUp() {
        service = new ResourcesService(genericResourceService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateRejectsNullBody() {
        assertThrows(ResourceException.class, () -> service.update("doc-1", null));
    }

    @Test
    void updateMarksDocumentCuratedAndUpdatesMetadata() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(oidcAuthentication());
        Document existing = new Document();
        existing.setId("doc-1");
        existing.setMetadata(new DocumentMetadata());
        JsonNode docInfo = new ObjectMapper().readTree("{\"title\":\"Updated\"}");

        when(genericResourceService.get("document", "doc-1")).thenReturn(existing);
        when(genericResourceService.update(eq("document"), eq("doc-1"), any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(2));

        Document updated = service.update("doc-1", docInfo);

        assertSame(docInfo, updated.getDocInfo());
        assertEquals(true, updated.isCurated());
        assertEquals("user@example.org", updated.getMetadata().getModifiedBy());
        assertNotNull(updated.getMetadata().getModificationDate());
    }

    @Test
    void setStatusRejectsPendingAssignments() {
        assertThrows(ResourceException.class, () -> service.setStatus("doc-1", Document.Status.PENDING));
    }

    @Test
    void getRecommendationsDelegatesToGenericResourceService() {
        when(genericResourceService.recommend(any(), eq("doc-1"))).thenReturn(List.of(new Document()));

        var result = service.getRecommendations(null, "doc-1");

        assertEquals(1, result.size());
        verify(genericResourceService).recommend(null, "doc-1");
    }

    private Authentication oidcAuthentication() {
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of(
                        "sub", "sub-1",
                        "email", "user@example.org",
                        "given_name", "User",
                        "family_name", "Example",
                        "name", "User Example"
                )
        );
        DefaultOidcUser principal = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)),
                idToken,
                "email"
        );
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                "token",
                principal.getAuthorities()
        );
    }
}
