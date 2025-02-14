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
package eu.openaire.observatory.configuration.security;

import eu.openaire.observatory.configuration.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthSuccessHandler.class);

    private final ApplicationProperties applicationProperties;

    @Autowired
    public AuthSuccessHandler(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Cookie cookie = new Cookie("AccessToken", ((OidcUser) authentication.getPrincipal()).getIdToken().getTokenValue());
        cookie.setMaxAge(createCookieMaxAge(authentication));
        cookie.setPath("/");
//        cookie.setSecure(true);

        response.addCookie(cookie);
        response.sendRedirect(applicationProperties.getLoginRedirect());
    }

    private int createCookieMaxAge(Authentication authentication) {
        Integer age = getExp(authentication);
        return age != null ? age : 3600;
    }

    private Integer getExp(Authentication authentication) {
        OidcUser user = ((OidcUser) authentication.getPrincipal());
        if (user.getAttribute("exp") instanceof Instant) {
            Instant exp = user.getAttribute("exp");
            int age = (int) (exp.getEpochSecond() - (new Date().getTime() / 1000));
            return age;
        }
        return null;
    }
}
