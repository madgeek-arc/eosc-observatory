package eu.openaire.observatory.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void ofBuildsUserFromOidcAuthentication() {
        Authentication authentication = oidcAuthentication("User@Example.com");

        User user = User.of(authentication);

        assertThat(user.getEmail()).isEqualTo("User@Example.com");
        assertThat(user.getId()).isEqualTo("user@example.com");
    }

    @Test
    void getIdNormalizesEmailToLowerCase() {
        assertThat(User.getId(oidcAuthentication("User@Example.com"))).isEqualTo("user@example.com");
    }

    @Test
    void ofBuildsUserFromOAuth2AuthenticationToken() {
        Authentication authentication = oauth2Authentication("Other@Example.com");

        User user = User.of(authentication);

        assertThat(user.getEmail()).isEqualTo("Other@Example.com");
        assertThat(user.getId()).isEqualTo("other@example.com");
    }

    @Test
    void getIdRejectsMissingEmail() {
        assertThatThrownBy(() -> User.getId(null))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessageContaining("not authenticated");
    }

    @Test
    void ofRejectsUnsupportedAuthentication() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated("user", "pw", List.of());

        assertThatThrownBy(() -> User.of(authentication))
                .isInstanceOf(InsufficientAuthenticationException.class)
                .hasMessageContaining("Insufficient user authentication");
    }

    private static Authentication oidcAuthentication(String email) {
        Map<String, Object> claims = Map.of(
                "sub", "sub-1",
                "email", email,
                "given_name", "User",
                "family_name", "Example",
                "name", "User Example"
        );
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), claims);
        DefaultOidcUser principal = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken);
        return UsernamePasswordAuthenticationToken.authenticated(principal, "n/a", principal.getAuthorities());
    }

    private static Authentication oauth2Authentication(String email) {
        DefaultOAuth2User principal = new DefaultOAuth2User(
                List.of(new OAuth2UserAuthority(Map.of("email", email))),
                Map.of("email", email),
                "email"
        );
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "test-client");
    }
}
