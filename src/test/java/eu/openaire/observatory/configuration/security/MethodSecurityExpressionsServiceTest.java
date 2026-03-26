package eu.openaire.observatory.configuration.security;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.service.AdministratorService;
import eu.openaire.observatory.service.CoordinatorService;
import eu.openaire.observatory.service.SecurityService;
import eu.openaire.observatory.service.StakeholderService;
import eu.openaire.observatory.service.SurveyAnswerCrudService;
import eu.openaire.observatory.service.UserService;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MethodSecurityExpressionsServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private SecurityService securityService;
    @Mock
    private CoordinatorService coordinatorService;
    @Mock
    private AdministratorService administratorService;
    @Mock
    private StakeholderService stakeholderService;
    @Mock
    private ModelService modelService;
    @Mock
    private SurveyAnswerCrudService surveyAnswerCrudService;

    private MethodSecurityExpressionsService service;

    @BeforeEach
    void setUp() {
        service = new MethodSecurityExpressionsService(
                userService,
                securityService,
                coordinatorService,
                administratorService,
                stakeholderService,
                modelService,
                surveyAnswerCrudService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userIsStakeholderMemberReturnsFalseWhenStakeholderMissing() {
        when(stakeholderService.get("sh-1")).thenThrow(new ResourceNotFoundException("missing"));

        boolean result = service.userIsStakeholderMember("user@example.org", "sh-1");

        assertFalse(result);
    }

    @Test
    void userIsStakeholderMemberChecksBothMembersAndAdmins() {
        Stakeholder stakeholder = new Stakeholder();
        stakeholder.setMembers(Set.of("member@example.org"));
        stakeholder.setAdmins(Set.of("admin@example.org"));
        when(stakeholderService.get("sh-1")).thenReturn(stakeholder);

        assertTrue(service.userIsStakeholderMember("member@example.org", "sh-1"));
        assertTrue(service.userIsStakeholderMember("admin@example.org", "sh-1"));
        assertFalse(service.userIsStakeholderMember("other@example.org", "sh-1"));
    }

    @Test
    void hasAccessAllowsAdminsWithoutPermissionLookup() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "principal",
                        "credentials",
                        List.of(new SimpleGrantedAuthority("ADMIN"))
                )
        );

        boolean result = service.hasAccess("resource-1", "write");

        assertTrue(result);
    }

    @Test
    void hasAccessResolvesResponseEntityBodyAndDelegatesToSecurityService() {
        SecurityContextHolder.getContext().setAuthentication(oidcAuthentication("user@example.org"));
        when(securityService.hasPermission(SecurityContextHolder.getContext().getAuthentication(), "read", "resource-1"))
                .thenReturn(true);

        boolean result = service.hasAccess(ResponseEntity.ok(new TestIdentifiable("resource-1")), "READ");

        assertTrue(result);
        verify(securityService).hasPermission(SecurityContextHolder.getContext().getAuthentication(), "read", "resource-1");
    }

    @Test
    void canReadDocumentsAllowsApprovedOnlyWithoutAdminAccess() {
        assertTrue(service.canReadDocuments(Document.Status.APPROVED));
        assertFalse(service.canReadDocuments(Document.Status.PENDING));
    }

    private UsernamePasswordAuthenticationToken oidcAuthentication(String email) {
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of(
                        "sub", "sub-1",
                        "email", email,
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
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                "token",
                principal.getAuthorities()
        );
    }

    private record TestIdentifiable(String id) implements eu.openaire.observatory.service.Identifiable<String> {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public void setId(String id) {
            throw new UnsupportedOperationException();
        }
    }
}
