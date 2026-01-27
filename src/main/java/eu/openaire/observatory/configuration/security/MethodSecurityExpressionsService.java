/*
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

import eu.openaire.observatory.domain.*;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.service.*;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.catalogue.utils.ReflectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MethodSecurityExpressionsService implements MethodSecurityExpressions {

    private static final Logger logger = LoggerFactory.getLogger(MethodSecurityExpressionsService.class);

    private final UserService userService;
    private final SecurityService securityService;
    private final CoordinatorService coordinatorService;
    private final AdministratorService administratorService;
    private final StakeholderService stakeholderService;
    private final ModelService modelService;
    private final SurveyAnswerCrudService surveyAnswerCrudService;

    @Lazy
    public MethodSecurityExpressionsService(UserService userService,
                                            SecurityService securityService,
                                            CoordinatorService coordinatorService,
                                            AdministratorService administratorService,
                                            StakeholderService stakeholderService,
                                            ModelService modelService,
                                            SurveyAnswerCrudService surveyAnswerCrudService) {
        this.userService = userService;
        this.securityService = securityService;
        this.coordinatorService = coordinatorService;
        this.administratorService = administratorService;
        this.stakeholderService = stakeholderService;
        this.modelService = modelService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;

    }

    @Override
    public boolean userIsMemberOfGroup(String userId, String groupId) {
        return userIsMemberOfGroup(userId, Collections.singletonList(groupId));
    }

    @Override
    public boolean userIsMemberOfGroup(String userId, List<String> groupIds) {
        UserInfo info = userService.getUserInfo(userId);
        Set<String> userGroups = new HashSet<>();
        userGroups.addAll(info.getAdministrators().stream().map(Administrator::getId).collect(Collectors.toSet()));
        userGroups.addAll(info.getCoordinators().stream().map(Coordinator::getId).collect(Collectors.toSet()));
        userGroups.addAll(info.getStakeholders().stream().map(Stakeholder::getId).collect(Collectors.toSet()));
        return userGroups.containsAll(groupIds);
    }

    @Override
    public boolean userIsStakeholderMember(String userId, String stakeholderId) {
        if (stakeholderId == null || userId == null) {
            return false;
        }
        Stakeholder stakeholder;
        try {
            stakeholder = stakeholderService.get(stakeholderId);
        } catch (ResourceNotFoundException e) {
            return false;
        }
        Set<String> emails = new HashSet<>();
        if (stakeholder.getMembers() != null) {
            emails.addAll(stakeholder.getMembers());
        }
        if (stakeholder.getAdmins() != null) {
            emails.addAll(stakeholder.getAdmins());
        }
        return emails.contains(userId);
    }

    @Override
    public boolean isStakeholderMember(String stakeholderId) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsStakeholderMember(user.getId(), stakeholderId);
    }

    @Override
    public boolean userIsStakeholderManager(String userId, String stakeholderId) {
        if (stakeholderId == null || userId == null) {
            return false;
        }
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        return stakeholder.getAdmins() != null && stakeholder.getAdmins().contains(userId);
    }

    @Override
    public boolean isStakeholderManager(String stakeholderId) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsStakeholderManager(user.getId(), stakeholderId);
    }

    @Override
    public boolean userIsCoordinator(String userId, String coordinatorId) {
        if (coordinatorId == null || userId == null) {
            return false;
        }
        Coordinator coordinator;
        try {
            coordinator = coordinatorService.get(coordinatorId);
        } catch (ResourceNotFoundException e) {
            return false;
        }
        return coordinator.getUsers() != null && coordinator.getUsers().contains(userId);
    }

    @Override
    public boolean isCoordinator(String coordinatorId) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsCoordinator(user.getId(), coordinatorId);
    }

    @Override
    public boolean userIsCoordinatorOfType(String userId, String type) {
        if (type == null || userId == null) {
            return false;
        }
        FacetFilter ff = new FacetFilter();
        ff.addFilter("users", userId);
        ff.addFilter("type", type);
        List<Coordinator> coordinators = coordinatorService.getAll(ff).getResults();
        return !coordinators.isEmpty();
    }

    @Override
    public boolean isCoordinatorOfType(String type) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsCoordinatorOfType(user.getId(), type);
    }

    @Override
    public boolean userIsCoordinatorOfStakeholder(String userId, String stakeholderId) {
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        return userIsCoordinatorOfType(userId, stakeholder.getType());
    }

    @Override
    public boolean isCoordinatorOfStakeholder(String stakehodlerId) {
        User user = userService.get(User.getId(getAuthentication()));
        Stakeholder stakeholder = stakeholderService.get(stakehodlerId);
        return userIsCoordinatorOfType(user.getId(), stakeholder.getType());
    }

    @Override
    public boolean hasStakeholderManagerAccess(Object surveyAnswer) {
        SurveyAnswer answer;
        if (surveyAnswer instanceof String) {
            answer = surveyAnswerCrudService.get((String) surveyAnswer);
        } else if (surveyAnswer instanceof SurveyAnswer) {
            answer = (SurveyAnswer) surveyAnswer;
        } else {
            throw new RuntimeException("Unsupported object");
        }
        User user = userService.get(User.getId(getAuthentication()));
        return userIsStakeholderManagerOfType(user, answer.getType());
    }

    @Override
    public boolean hasStakeholderManagerAccessOnSurvey(String surveyId) {
        if (surveyId == null) {
            return false;
        }
        Model survey = modelService.get(surveyId);
        User user = userService.get(User.getId(getAuthentication()));
        return userIsStakeholderManagerOfType(user, survey.getType());
    }

    private boolean userIsStakeholderManagerOfType(User user, String type) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("admins", user.getId());
        filter.addFilter("type", type);
        Browsing<Stakeholder> stakeholder = stakeholderService.getAll(filter);
        if (stakeholder.getTotal() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasCoordinatorAccessOnSurvey(String surveyId) {
        if (surveyId == null) {
            return false;
        }
        Model survey = modelService.get(surveyId);
        User user = userService.get(User.getId(getAuthentication()));
        return userIsCoordinatorOfType(user, survey.getType());
    }

    @Override
    public boolean hasCoordinatorAccess(Object surveyAnswer) {
        SurveyAnswer answer;
        if (surveyAnswer == null) {
            return false;
        } else if (surveyAnswer instanceof String) {
            answer = surveyAnswerCrudService.get((String) surveyAnswer);
        } else if (surveyAnswer instanceof SurveyAnswer) {
            answer = (SurveyAnswer) surveyAnswer;
        } else {
            throw new RuntimeException("Unsupported object");
        }
        User user = userService.get(User.getId(getAuthentication()));
        return userIsCoordinatorOfType(user, answer.getType());
    }

    private boolean userIsCoordinatorOfType(User user, String type) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("users", user.getId());
        filter.addFilter("type", type);
        Browsing<Coordinator> coordinators = coordinatorService.getAll(filter);
        if (coordinators.getTotal() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasAccess(Object resource, Object permission) {
        Authentication authentication = getAuthentication();
        if ((authentication == null) || (resource == null) || !(permission instanceof String)) {
            return false;
        }

        // convert to Object when resource is a returnObject (@PostConstruct)
        if (resource instanceof ResponseEntity) {
            resource = ((ResponseEntity<?>) resource).getBody();
            if (resource == null) { // FIXME: throw ResourceNotFound exception ?
                return false;
            }
        }

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
        return isAdmin(authentication) ||
                securityService.hasPermission(authentication, permission.toString().toLowerCase(), resourceId);
    }

    @Override
    public boolean userIsAdministrator(String userId, String administratorId) {
        if (administratorId == null || userId == null) {
            return false;
        }
        Administrator administrator;
        try {
            administrator = administratorService.get(administratorId);
        } catch (ResourceNotFoundException e) {
            return false;
        }
        return administrator.getUsers() != null && administrator.getUsers().contains(userId);
    }

    @Override
    public boolean isAdministrator(String administratorId) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsAdministrator(user.getId(), administratorId);
    }

    @Override
    public boolean userIsAdministratorOfType(String userId, String type) {
        if (type == null || userId == null) {
            return false;
        }
        FacetFilter ff = new FacetFilter();
        ff.addFilter("users", userId);
        ff.addFilter("type", type);
        List<Administrator> administrators = administratorService.getAll(ff).getResults();
        return !administrators.isEmpty();
    }

    @Override
    public boolean isAdministratorOfCoordinator(String coordinatorId) {
        Coordinator coordinator =  coordinatorService.get(coordinatorId);
        User user = userService.get(User.getId(getAuthentication()));
        return userIsAdministratorOfType(user.getId(), coordinator.getType());
    }

    @Override
    public boolean isAdministratorOfStakeholder(String stakehodlerId) {
        Stakeholder stakeholder = stakeholderService.get(stakehodlerId);
        User user = userService.get(User.getId(getAuthentication()));
        return userIsAdministratorOfType(user.getId(), stakeholder.getType());
    }

    @Override
    public boolean isAdministratorOfType(String type) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsAdministratorOfType(user.getId(), type);
    }

    @Override
    public boolean isAdmin(Authentication auth) {
        if (auth != null) {
            for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
                if (grantedAuth.getAuthority().contains("ADMIN")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canReadDocuments(Document.Status status) {
        return (status != null && status.equals(Document.Status.APPROVED)) || isAdministratorOfType("eosc-sb");
    }

    @Override
    public boolean canReadDocument(String documentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWriteDocument(String documentId) {
        throw new UnsupportedOperationException();
    }


    /* ********************************************** */
    /*              Other Help Methods                */
    /* ********************************************** */

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
