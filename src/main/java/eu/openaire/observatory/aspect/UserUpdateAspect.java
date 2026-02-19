/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

package eu.openaire.observatory.aspect;

import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.service.AdministratorService;
import eu.openaire.observatory.service.CoordinatorService;
import eu.openaire.observatory.service.StakeholderService;
import eu.openaire.observatory.service.UserService;
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
    private final AdministratorService administratorService;

    @Autowired
    public UserUpdateAspect(UserService userService,
                            StakeholderService stakeholderService,
                            CoordinatorService coordinatorService,
                            AdministratorService administratorService) {
        this.userService = userService;
        this.stakeholderService = stakeholderService;
        this.coordinatorService = coordinatorService;
        this.administratorService = administratorService;
    }

    @Before("execution(* eu.openaire.observatory.configuration.security.AuthSuccessHandler.onAuthenticationSuccess(..))")
    public void updateUser(JoinPoint joinPoint) {
        Authentication authentication = (Authentication) joinPoint.getArgs()[joinPoint.getArgs().length - 1];
        try {
            // update user info
            User user = User.of(authentication);
            if (!stakeholderService.getWithFilter("users", user.getId()).isEmpty()
                    || !coordinatorService.getWithFilter("users", user.getId()).isEmpty()
                    || !administratorService.getWithFilter("users", user.getId()).isEmpty()) {
                logger.debug("Updating user information");
                userService.updateUserDetails(authentication);
            }
        } catch (RuntimeException e) {
            logger.error("Could not update user info..", e);
        }
    }
}
