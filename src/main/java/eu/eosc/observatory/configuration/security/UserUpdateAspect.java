package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.service.UserService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UserUpdateAspect {

    private static final Logger logger = LogManager.getLogger(UserUpdateAspect.class);

    private final UserService userService;

    @Autowired
    public UserUpdateAspect(UserService userService) {
        this.userService = userService;
    }

    @Before("execution(* eu.eosc.observatory.configuration.security.AuthSuccessHandler.onAuthenticationSuccess(..))")
    public void updateUser(JoinPoint joinPoint) {
        Authentication authentication = (Authentication) joinPoint.getArgs()[joinPoint.getArgs().length-1];
        logger.info(String.format("Successful Login [authentication: %s]", authentication.toString()));
        // update user info
        try {
            userService.updateUserInfo(authentication);
        } catch (RuntimeException e) {
            logger.error("Could not update user info..", e);
        }
    }
}
