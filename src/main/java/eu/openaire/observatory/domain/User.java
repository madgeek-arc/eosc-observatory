/**
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.domain;

import eu.openaire.observatory.service.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;

public class User implements Identifiable<String> {
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    private String sub;
    private String email;
    private String name;
    private String surname;
    private String fullname;
    private List<PolicyAccepted> policiesAccepted;
    private Profile profile;
    private Settings settings = new Settings();

    public User() {
    }

    public static User of(Authentication auth) {
        logger.trace(String.format("Creating UserService from Authentication: %n%s", auth));
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
        logger.debug(String.format("UserService from Authentication: %s", user));
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
        if (email != null) {
            email = email.toLowerCase();
        }
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

    public List<PolicyAccepted> getPoliciesAccepted() {
        return policiesAccepted;
    }

    public void setPoliciesAccepted(List<PolicyAccepted> policiesAccepted) {
        this.policiesAccepted = policiesAccepted;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Settings getSettings() {
        return settings;
    }

    public User setSettings(Settings settings) {
        this.settings = settings;
        return this;
    }

    public static String getId(Authentication auth) {
        logger.trace(String.format("Retrieving id from Authentication: %n%s", auth));
        String id = null;
        if (auth == null) {
            throw new InsufficientAuthenticationException("You are not authenticated, please log in.");
        } else if (auth.getPrincipal() instanceof OidcUser) {
            OidcUser principal = ((OidcUser) auth.getPrincipal());
            id = principal.getEmail();
        } else if (auth instanceof OAuth2AuthenticationToken) {
            OAuth2User principal = ((OAuth2AuthenticationToken) auth).getPrincipal();
            id = principal.getAttribute("email");
        } else {
            throw new InsufficientAuthenticationException("Could not create user. Insufficient user authentication");
        }
        if (id == null || "".equals(id)) {
            logger.error("Problem getting user id. Authentication: {}", auth);
            throw new InsufficientAuthenticationException("Could not obtain user id through authentication.");
        }
        return id.toLowerCase();
    }

    @Override
    public String getId() {
        return this.email != null ? this.email.toLowerCase() : null;
    }

    @Override
    public void setId(String id) {
        this.setEmail(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "sub='" + sub + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", fullname='" + fullname + '\'' +
                ", policiesAccepted=" + policiesAccepted +
                '}';
    }
}
