package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.permissions.PermissionService;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private PermissionService permissionService;
    @Mock
    private UserService userService;

    private SecurityService service;
    private Authentication authentication;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SecurityService(permissionService, userService);
        authentication = oidcAuthentication("user@example.org");
        user = new User();
        user.setEmail("user@example.org");
    }

    @Test
    void canReadDelegatesToReadPermission() {
        when(userService.get("user@example.org")).thenReturn(user);
        when(permissionService.canRead("user@example.org", "res-1")).thenReturn(true);

        boolean result = service.canRead(authentication, "res-1");

        assertTrue(result);
        verify(permissionService).canRead("user@example.org", "res-1");
    }

    @Test
    void canWriteDelegatesToWritePermission() {
        when(userService.get("user@example.org")).thenReturn(user);
        when(permissionService.canWrite("user@example.org", "res-1")).thenReturn(true);

        boolean result = service.canWrite(authentication, "res-1");

        assertTrue(result);
        verify(permissionService).canWrite("user@example.org", "res-1");
        verifyNoMoreInteractions(permissionService);
    }

    @Test
    void canManageDelegatesToManagePermission() {
        when(userService.get("user@example.org")).thenReturn(user);
        when(permissionService.canManage("user@example.org", "res-1")).thenReturn(true);

        boolean result = service.canManage(authentication, "res-1");

        assertTrue(result);
        verify(permissionService).canManage("user@example.org", "res-1");
    }

    @Test
    void hasPermissionDelegatesToRequestedAction() {
        when(userService.get("user@example.org")).thenReturn(user);
        when(permissionService.hasPermission("user@example.org", "publish", "res-1")).thenReturn(true);

        boolean result = service.hasPermission(authentication, "publish", "res-1");

        assertTrue(result);
        verify(permissionService).hasPermission("user@example.org", "publish", "res-1");
    }

    @Test
    void returnsFalseWhenUserCannotBeResolved() {
        when(userService.get("user@example.org")).thenThrow(new ResourceNotFoundException("missing"));

        assertFalse(service.canRead(authentication, "res-1"));
        assertFalse(service.canWrite(authentication, "res-1"));
        assertFalse(service.canManage(authentication, "res-1"));
        assertFalse(service.hasPermission(authentication, "publish", "res-1"));
    }

    private Authentication oidcAuthentication(String email) {
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
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                "token",
                principal.getAuthorities()
        );
    }
}
