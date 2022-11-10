package eu.eosc.observatory.aspect;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.CoordinatorService;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.UserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;


@Aspect
@Component
public class UserUpdateAspect {

    private static final Logger logger = LoggerFactory.getLogger(UserUpdateAspect.class);

    private final UserService userService;
    private final StakeholderService stakeholderService;
    private final CoordinatorService coordinatorService;

    @Autowired
    public UserUpdateAspect(UserService userService,
                            StakeholderService stakeholderService,
                            CoordinatorService coordinatorService) {
        this.userService = userService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
    }

    @Before("execution(* eu.eosc.observatory.configuration.security.AuthSuccessHandler.onAuthenticationSuccess(..))")
    public void updateUser(JoinPoint joinPoint) {
        Authentication authentication = (Authentication) joinPoint.getArgs()[joinPoint.getArgs().length - 1];
        logger.info(String.format("Successful Login [authentication: %s]", authentication.toString()));
        // update user info
        try {
            User user = User.of(authentication);
            if (!stakeholderService.getWithFilter("managers", user.getId()).isEmpty()
                    || !stakeholderService.getWithFilter("contributors", user.getId()).isEmpty()
                    || !coordinatorService.getWithFilter("members", user.getId()).isEmpty()) {
                userService.updateUserDetails(authentication);
            }
        } catch (RuntimeException e) {
            logger.error("Could not update user info..", e);
        }
    }
}
