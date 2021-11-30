package eu.eosc.observatory.domain;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class User {
    private static final Logger logger = LogManager.getLogger(User.class);

    private String sub;
    private String email;
    private String name;
    private String surname;
    private String fullname;

    public User() {
    }

    public static User of(Authentication auth) {
        logger.trace(String.format("Creating User from Authentication: %n%s", auth));
        User user = new User();
        if (auth == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (auth.getPrincipal() instanceof OidcUser) {
            OidcUser principal = ((OidcUser) auth.getPrincipal());
            user.sub = principal.getSubject();
            user.email = principal.getEmail();
            user.name = principal.getGivenName();
            user.surname = principal.getFamilyName();
            user.fullname = principal.getFullName();
        } else if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User principal = ((OAuth2AuthenticationToken) auth).getPrincipal();
            user.sub = principal.getAttribute("subject");
            user.email = principal.getAttribute("email");
            user.name = principal.getAttribute("givenName");
            user.surname = principal.getAttribute("familyName");
            user.fullname = principal.getAttribute("fullName");
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
        logger.debug(String.format("User from Authentication: %s", user));
        return user;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
}
