package eu.openaire.observatory.domain;

import eu.openaire.observatory.utils.OidcTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

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
        return OidcTestUtils.oidcAuthentication(email);
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
