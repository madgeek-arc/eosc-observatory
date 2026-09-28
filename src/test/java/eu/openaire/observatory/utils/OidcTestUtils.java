package eu.openaire.observatory.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OidcTestUtils {

    private OidcTestUtils() {}

    public static UsernamePasswordAuthenticationToken oidcAuthentication(String email) {
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
