package eu.openaire.observatory.service;

import eu.openaire.observatory.configuration.ApplicationProperties;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.domain.Roles;
import eu.openaire.observatory.domain.User;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JWSInvitationServiceTest {

    @Mock
    private StakeholderService stakeholderService;
    @Mock
    private UserService userService;
    @Mock
    private MethodSecurityExpressions securityExpressions;

    private JWSInvitationService service;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setJwsSigningSecret("1234567890123456789012345678901234567890123456789012345678901234");
        service = new JWSInvitationService(stakeholderService, userService, properties, securityExpressions);
    }

    @Test
    void createInvitationProducesSignedToken() {
        User inviter = new User();
        inviter.setEmail("inviter@example.org");

        String token = service.createInvitation(
                inviter,
                "Invitee@Example.org",
                Roles.Stakeholder.CONTRIBUTOR.name(),
                "sh-1",
                new Date(System.currentTimeMillis() + 60_000L)
        );

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        assertTrue(token.contains("."));
    }

    @Test
    void acceptInvitationRejectsMalformedToken() {
        assertThrows(ResourceException.class, () -> service.acceptInvitation("invalid-token", oidcAuthentication("invitee@example.org")));
    }

    @Test
    void acceptInvitationRejectsWrongAuthenticatedUser() {
        User inviter = new User();
        inviter.setEmail("inviter@example.org");
        String token = service.createInvitation(
                inviter,
                "invitee@example.org",
                Roles.Stakeholder.CONTRIBUTOR.name(),
                "sh-1",
                new Date(System.currentTimeMillis() + 60_000L)
        );

        assertThrows(ResourceException.class, () -> service.acceptInvitation(token, oidcAuthentication("other@example.org")));
    }

    @Test
    void acceptInvitationAddsAdminForAuthorizedManagerInvite() {
        User inviter = new User();
        inviter.setEmail("coordinator@example.org");
        UsernamePasswordAuthenticationToken authentication = oidcAuthentication("invitee@example.org");
        String token = service.createInvitation(
                inviter,
                "invitee@example.org",
                Roles.Stakeholder.MANAGER.name(),
                "sh-1",
                new Date(System.currentTimeMillis() + 60_000L)
        );

        when(securityExpressions.userIsCoordinatorOfStakeholder("coordinator@example.org", "sh-1")).thenReturn(true);

        boolean accepted = service.acceptInvitation(token, authentication);

        assertTrue(accepted);
        verify(stakeholderService).addAdmin("sh-1", "invitee@example.org");
        verify(userService).add(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void acceptInvitationReturnsFalseWhenContributorInviterLacksPermission() {
        User inviter = new User();
        inviter.setEmail("manager@example.org");
        String token = service.createInvitation(
                inviter,
                "invitee@example.org",
                Roles.Stakeholder.CONTRIBUTOR.name(),
                "sh-1",
                new Date(System.currentTimeMillis() + 60_000L)
        );
        when(securityExpressions.userIsStakeholderManager("manager@example.org", "sh-1")).thenReturn(false);
        when(securityExpressions.userIsCoordinatorOfStakeholder("manager@example.org", "sh-1")).thenReturn(false);

        boolean accepted = service.acceptInvitation(token, oidcAuthentication("invitee@example.org"));

        assertFalse(accepted);
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
}
