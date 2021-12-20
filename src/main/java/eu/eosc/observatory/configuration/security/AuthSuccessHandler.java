package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.configuration.ApplicationProperties;
import eu.eosc.observatory.service.UserService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;

@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LogManager.getLogger(AuthSuccessHandler.class);

    private final UserService userService;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public AuthSuccessHandler(UserService userService, ApplicationProperties applicationProperties) {
        this.userService = userService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        AuthenticationSuccessHandler.super.onAuthenticationSuccess(request, response, chain, authentication);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        postLogin(authentication);

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

    private void postLogin(Authentication authentication) {
        logger.info(String.format("Successful Login [authentication: %s]", authentication.toString()));
        userService.updateUserInfo(authentication);
    }
}
