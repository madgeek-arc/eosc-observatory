package eu.openaire.observatory.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.configuration.security.MethodSecurityExpressions;
import eu.openaire.observatory.domain.UserGroup;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.Browsing;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsQuerySecurityTest {

    @Mock
    private MethodSecurityExpressions securityExpressions;
    @Mock
    private GenericResourceService genericResourceService;

    private StatsQuerySecurity service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = oidcAuthentication();

        service = new StatsQuerySecurity(
                securityExpressions,
                genericResourceService,
                statsToolProperties(),
                new ObjectMapper()
        );
    }

    @Test
    void authorizeAllowsOpenQueries() {
        boolean result = service.authorize(queryJson("open.metrics"), authentication);

        assertTrue(result);
    }

    @Test
    void authorizeDelegatesClosedQueriesToAdminCheck() {
        when(securityExpressions.isAdmin(authentication)).thenReturn(false);

        boolean result = service.authorize(queryJson("admin.metrics"), authentication);

        assertFalse(result);
        verify(securityExpressions).isAdmin(authentication);
    }

    @Test
    void authorizeAllowsRestrictedQueriesWhenUserBelongsToConfiguredGroup() {
        Browsing<Object> browsing = new Browsing<>();
        browsing.setResults(List.of(new Object()));
        when(genericResourceService.getResults(any())).thenReturn(browsing);

        boolean result = service.authorize(queryJson("restricted.metrics"), authentication);

        assertTrue(result);
        verify(genericResourceService).getResults(any());
    }

    @Test
    void authorizeRejectsMalformedJson() {
        assertThrows(RuntimeException.class, () -> service.authorize("{bad json", authentication));
    }

    private StatsToolProperties statsToolProperties() {
        StatsToolProperties properties = new StatsToolProperties();

        StatsToolProperties.QueryAccess open = new StatsToolProperties.QueryAccess();
        open.setQueryPattern("open\\..*");
        open.setAccess(StatsToolProperties.Access.OPEN);

        StatsToolProperties.QueryAccess closed = new StatsToolProperties.QueryAccess();
        closed.setQueryPattern("admin\\..*");
        closed.setAccess(StatsToolProperties.Access.CLOSED);

        StatsToolProperties.Group group = new StatsToolProperties.Group()
                .setName("stakeholder")
                .setRole("members")
                .setType(UserGroup.GroupType.AI);
        StatsToolProperties.QueryAccess restricted = new StatsToolProperties.QueryAccess();
        restricted.setQueryPattern("restricted\\..*");
        restricted.setAccess(StatsToolProperties.Access.RESTRICTED);
        restricted.setGroups(List.of(group));

        properties.setQueryAccess(List.of(open, closed, restricted));
        return properties;
    }

    private String queryJson(String name) {
        return """
                {
                  "series": [
                    {
                      "query": {
                        "name": "%s"
                      }
                    }
                  ]
                }
                """.formatted(name);
    }

    private Authentication oidcAuthentication() {
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of(
                        "sub", "sub-1",
                        "email", "user@example.org",
                        "given_name", "User",
                        "family_name", "Example",
                        "name", "User Example"
                )
        );
        DefaultOidcUser principal = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "email");
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal,
                "token",
                principal.getAuthorities()
        );
    }
}
