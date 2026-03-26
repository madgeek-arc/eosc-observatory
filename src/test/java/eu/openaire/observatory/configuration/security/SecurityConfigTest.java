package eu.openaire.observatory.configuration.security;

import eu.openaire.observatory.configuration.ApplicationProperties;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties();
        properties.setAdmins(Set.of("admin@example.org"));
        properties.setLogoutRedirect("http://localhost:4200");
        securityConfig = new SecurityConfig((request, response, authentication) -> {
        }, clientRegistrationRepository, properties);
    }

    @Test
    void userAuthoritiesMapperGrantsAdminForConfiguredOidcEmail() {
        GrantedAuthoritiesMapper mapper = securityConfig.userAuthoritiesMapper();
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("email", "admin@example.org", "sub", "sub-1")
        );

        Set<GrantedAuthority> authorities = (Set<GrantedAuthority>) mapper.mapAuthorities(Set.of(new OidcUserAuthority(idToken)));

        assertTrue(authorities.stream().anyMatch(a -> "ADMIN".equals(a.getAuthority())));
    }

    @Test
    void userAuthoritiesMapperUsesUserInfoAndOAuthAttributes() {
        GrantedAuthoritiesMapper mapper = securityConfig.userAuthoritiesMapper();
        OidcIdToken idToken = new OidcIdToken(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("sub", "sub-1", "email", "non-admin@example.org")
        );
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("email", "admin@example.org"));
        OAuth2UserAuthority oauth2 = new OAuth2UserAuthority(Map.of("email", "admin@example.org"));

        Set<GrantedAuthority> authorities = (Set<GrantedAuthority>) mapper.mapAuthorities(Set.of(
                new OidcUserAuthority(idToken, userInfo),
                oauth2
        ));

        assertEquals(1, authorities.size());
        assertEquals("ADMIN", authorities.iterator().next().getAuthority());
    }

    @Test
    void csrfCookieFilterAddsReadableCookieWhenTokenPresent() throws Exception {
        OncePerRequestFilter filter = (OncePerRequestFilter) ReflectionTestUtils.invokeMethod(securityConfig, "csrfCookieFilter");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        CsrfToken csrfToken = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "token-value");
        request.setAttribute("_csrf", csrfToken);

        filter.doFilter(request, response, chain);

        assertEquals("token-value", response.getCookie("XSRF-TOKEN").getValue());
        assertTrue(!response.getCookie("XSRF-TOKEN").isHttpOnly());
        verify(chain).doFilter(request, response);
    }
}
