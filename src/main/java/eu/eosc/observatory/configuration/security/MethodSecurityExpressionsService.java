package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.domain.*;
import eu.eosc.observatory.service.*;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.ui.domain.Model;
import gr.athenarc.catalogue.ui.service.ModelService;
import gr.athenarc.catalogue.utils.ReflectUtils;
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
    private final StakeholderService stakeholderService;
    private final ModelService modelService;
    private final SurveyAnswerCrudService surveyAnswerCrudService;

    @Lazy
    public MethodSecurityExpressionsService(UserService userService,
                                            SecurityService securityService,
                                            CoordinatorService coordinatorService,
                                            StakeholderService stakeholderService,
                                            ModelService modelService,
                                            SurveyAnswerCrudService surveyAnswerCrudService) {
        this.userService = userService;
        this.securityService = securityService;
        this.coordinatorService = coordinatorService;
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
        userGroups.addAll(info.getCoordinators().stream().map(Coordinator::getId).collect(Collectors.toSet()));
        userGroups.addAll(info.getStakeholders().stream().map(Stakeholder::getId).collect(Collectors.toSet()));
        return userGroups.containsAll(groupIds);
    }

    @Override
    public boolean userIsStakeholderMember(String userId, String stakeholderId) {
        if (stakeholderId == null || userId == null) {
            return false;
        }
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
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
    public boolean userIsCoordinatorMember(String userId, String coordinatorId) {
        if (coordinatorId == null || userId == null) {
            return false;
        }
        Coordinator coordinator = coordinatorService.get(coordinatorId);
        return coordinator.getMembers() != null && coordinator.getMembers().contains(userId);
    }

    @Override
    public boolean isCoordinatorMember(String coordinatorId) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsCoordinatorMember(user.getId(), coordinatorId);
    }

    @Override
    public boolean userIsCoordinatorMemberOfType(String userId, String type) {
        if (type == null || userId == null) {
            return false;
        }
        FacetFilter ff = new FacetFilter();
        ff.addFilter("members", userId);
        ff.addFilter("type", type);
        List<Coordinator> coordinators = coordinatorService.getAll(ff).getResults();
        return !coordinators.isEmpty();
    }

    @Override
    public boolean isCoordinatorMemberOfType(String type) {
        User user = userService.get(User.getId(getAuthentication()));
        return userIsCoordinatorMemberOfType(user.getId(), type);
    }

    @Override
    public boolean userIsCoordinatorMemberOfStakeholder(String userId, String stakeholderId) {
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        return userIsCoordinatorMemberOfType(userId, stakeholder.getType());
    }

    @Override
    public boolean isCoordinatorMemberOfStakeholder(String stakehodlerId) {
        User user = userService.get(User.getId(getAuthentication()));
        Stakeholder stakeholder = stakeholderService.get(stakehodlerId);
        return userIsCoordinatorMemberOfType(user.getId(), stakeholder.getType());
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
        return userIsManagerOfType(user, answer.getType());
    }

    @Override
    public boolean hasStakeholderManagerAccess(String surveyId) {
        if (surveyId == null) {
            return false;
        }
        Model survey = modelService.get(surveyId);
        User user = userService.get(User.getId(getAuthentication()));
        return userIsManagerOfType(user, survey.getType());
    }

    private boolean userIsManagerOfType(User user, String type) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("managers", user.getId());
        filter.addFilter("type", type);
        Browsing<Stakeholder> stakeholder = stakeholderService.getAll(filter);
        if (stakeholder.getTotal() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasCoordinatorAccess(String surveyId) {
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
        filter.addFilter("managers", user.getId());
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
            resource = ((ResponseEntity) resource).getBody();
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
    public boolean isAdmin(Authentication auth) {
        for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
            if (grantedAuth.getAuthority().contains("ADMIN")) {
                return true;
            }
        }
        return false;
    }


    /* ********************************************** */
    /*              Other Help Methods                */
    /* ********************************************** */

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
