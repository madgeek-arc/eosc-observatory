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
package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.service.Identifiable;
import eu.eosc.observatory.service.SecurityService;
import gr.athenarc.catalogue.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations, PermissionEvaluator, MethodSecurityExpressions {

    private static final Logger logger = LoggerFactory.getLogger(CustomMethodSecurityExpressionRoot.class);

    private final MethodSecurityExpressions securityExpressions;
    private final SecurityService securityService;

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public CustomMethodSecurityExpressionRoot(Authentication authentication,
                                              MethodSecurityExpressions securityExpressions,
                                              SecurityService securityService) {
        super(authentication);
        this.setPermissionEvaluator(this);
        this.securityExpressions = securityExpressions;
        this.securityService = securityService;
    }


    /* ********************************************** */
    /*      MethodSecurityExpressionOperations        */
    /* ********************************************** */

    @Override
    public void setFilterObject(Object filterObject) {
        this.filterObject = filterObject;
    }

    @Override
    public Object getFilterObject() {
        return filterObject;
    }

    @Override
    public void setReturnObject(Object o) {
        if (o != null) {
            if (o instanceof ResponseEntity<?>) {
                o = ((ResponseEntity<?>) o).getBody();
            }
        }
        this.returnObject = o;
    }

    @Override
    public Object getReturnObject() {
        return returnObject;
    }

    @Override
    public Object getThis() {
        return target;
    }



    /* ********************************************** */
    /*              PermissionEvaluator               */
    /* ********************************************** */

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        logger.debug("hasPermission(auth, targetDomainObject, permission)\nAuthentication: {}\nObject: {}\nPermission: {}", authentication, targetDomainObject, permission);
        if ((getAuthentication() == null) || (targetDomainObject == null) || !(permission instanceof String)) {
            return false;
        }
        String resourceId = getResourceId(targetDomainObject);
        return securityExpressions.isAdmin(authentication) ||
                securityService.hasPermission(authentication, ((String) permission).toLowerCase(), resourceId);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Not implemented");
    }


    /* ********************************************** */
    /*   Custom Security Expression Methods Wrapper   */
    /* ********************************************** */

    @Override
    public boolean userIsMemberOfGroup(String userId, String groupId) {
        return securityExpressions.userIsMemberOfGroup(userId, groupId);
    }

    @Override
    public boolean userIsMemberOfGroup(String userId, List<String> groupIds) {
        return securityExpressions.userIsMemberOfGroup(userId, groupIds);
    }

    @Override
    public boolean userIsStakeholderMember(String userId, String stakeholderId) {
        return securityExpressions.userIsStakeholderMember(userId, stakeholderId);
    }

    @Override
    public boolean isStakeholderMember(String stakeholderId) {
        return securityExpressions.isStakeholderMember(stakeholderId);
    }

    @Override
    public boolean userIsStakeholderManager(String userId, String stakeholderId) {
        return securityExpressions.userIsStakeholderManager(userId, stakeholderId);
    }

    @Override
    public boolean isStakeholderManager(String stakeholderId) {
        return securityExpressions.isStakeholderManager(stakeholderId);
    }

    @Override
    public boolean userIsCoordinator(String userId, String coordinatorId) {
        return securityExpressions.userIsCoordinator(userId, coordinatorId);
    }

    @Override
    public boolean isCoordinator(String coordinatorId) {
        return securityExpressions.isCoordinator(coordinatorId);
    }

    @Override
    public boolean isAdministrator(String adminId) {
        return securityExpressions.isAdministrator(adminId);
    }

    @Override
    public boolean userIsAdministratorOfType(String userId, String type) {
        return securityExpressions.userIsAdministratorOfType(userId, type);
    }

    @Override
    public boolean isAdministratorOfType(String type) {
        return securityExpressions.isAdministratorOfType(type);
    }

    @Override
    public boolean userIsCoordinatorOfType(String userId, String type) {
        return securityExpressions.userIsCoordinatorOfType(userId, type);
    }

    @Override
    public boolean isCoordinatorOfType(String type) {
        return securityExpressions.isCoordinatorOfType(type);
    }

    @Override
    public boolean userIsCoordinatorOfStakeholder(String userId, String stakehodlerId) {
        return securityExpressions.userIsCoordinatorOfStakeholder(userId, stakehodlerId);
    }

    @Override
    public boolean isCoordinatorOfStakeholder(String stakehodlerId) {
        return securityExpressions.isCoordinatorOfStakeholder(stakehodlerId);
    }

    @Override
    public boolean hasStakeholderManagerAccessOnSurvey(String surveyId) {
        return securityExpressions.hasStakeholderManagerAccessOnSurvey(surveyId);
    }

    @Override
    public boolean hasCoordinatorAccessOnSurvey(String surveyId) {
        return securityExpressions.hasCoordinatorAccessOnSurvey(surveyId);
    }

    @Override
    public boolean hasStakeholderManagerAccess(Object surveyAnswer) {
        return securityExpressions.hasStakeholderManagerAccess(surveyAnswer);
    }

    @Override
    public boolean hasCoordinatorAccess(Object surveyAnswer) {
        return securityExpressions.hasCoordinatorAccess(surveyAnswer);
    }

    @Override
    public boolean hasAccess(Object resource, Object permission) {
        return securityExpressions.hasAccess(resource, permission);
    }

    @Override
    public boolean userIsAdministrator(String userId, String administratorId) {
        return securityExpressions.userIsAdministrator(userId, administratorId);
    }

    @Override
    public boolean isAdmin(Authentication authentication) {
        return securityExpressions.isAdmin(authentication);
    }


    /* ********************************************** */
    /*              Other Help Methods                */
    /* ********************************************** */

    private String getResourceId(Object resource) {
        // get resource id
        String resourceId = null;
        if (resource instanceof String) {
            resourceId = resource.toString();
        } else if (resource instanceof Identifiable) {
            resourceId = ((Identifiable<String>) resource).getId();
        } else {
            try {
                resourceId = ReflectUtils.getId(resource.getClass(), resource);
            } catch (NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return resourceId;
    }
}
