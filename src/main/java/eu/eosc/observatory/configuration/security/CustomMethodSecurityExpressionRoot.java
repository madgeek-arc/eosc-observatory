package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.*;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import gr.athenarc.catalogue.ReflectUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations, PermissionEvaluator {

    private static final Logger logger = LogManager.getLogger(CustomMethodSecurityExpressionRoot.class);

    private final UserService userService;
    private final SecurityService securityService;
    private final CoordinatorService coordinatorService;
    private final StakeholderService stakeholderService;
    private final SurveyAnswerCrudService surveyAnswerCrudService;

    private Object filterObject;
    private Object returnObject;
    private Object target;

    public CustomMethodSecurityExpressionRoot(Authentication authentication,
                                              UserService userService,
                                              SecurityService securityService,
                                              CoordinatorService coordinatorService,
                                              StakeholderService stakeholderService,
                                              SurveyAnswerCrudService surveyAnswerCrudService) {
        super(authentication);
        this.setPermissionEvaluator(this);
        this.userService = userService;
        this.securityService = securityService;
        this.coordinatorService = coordinatorService;
        this.stakeholderService = stakeholderService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
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
        if ((this.authentication == null) || (targetDomainObject == null) || !(permission instanceof String)) {
            return false;
        }
        String resourceId = getResourceId(targetDomainObject);
        return isAdmin(authentication) ||
                securityService.hasPermission(authentication, permission.toString().toLowerCase(), resourceId);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Not implemented");
    }


    /* ********************************************** */
    /*       Custom Security Expression Methods       */
    /* ********************************************** */

    public boolean isStakeholderMember(String stakeholderId) {
        User user = userService.get(User.getId(this.authentication));
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        Set<String> emails = new HashSet<>();
        if (stakeholder.getContributors() != null) {
            emails.addAll(stakeholder.getContributors());
        }
        if (stakeholder.getManagers() != null) {
            emails.addAll(stakeholder.getManagers());
        }
        return emails.contains(user.getId());
    }

    public boolean isStakeholderManager(String stakeholderId) {
        User user = userService.get(User.getId(this.authentication));
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        return stakeholder.getManagers() != null && stakeholder.getManagers().contains(user.getId());
    }

    public boolean hasCoordinatorAccess(Object surveyAnswer) {
        SurveyAnswer answer;
        if (surveyAnswer instanceof String) {
            answer = surveyAnswerCrudService.get((String) surveyAnswer);
        } else if (surveyAnswer instanceof SurveyAnswer) {
            answer = (SurveyAnswer) surveyAnswer;
        } else {
            throw new RuntimeException("Unsupported object");
        }
        User user = userService.get(User.of(authentication).getId());
        FacetFilter filter = new FacetFilter();
        filter.addFilter("members", user.getId());
        filter.addFilter("type", answer.getType());
        Browsing<Coordinator> coordinators = coordinatorService.getAll(filter);
        if (coordinators.getTotal() > 0) {
            return true;
        }
        return false;
    }

    public boolean hasAccess(Object resource, Object permission) {
        if ((this.authentication == null) || (resource == null) || !(permission instanceof String)) {
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
            } catch (NoSuchFieldException e) {
                logger.error(e);
            }
        }
        return isAdmin(authentication) ||
                securityService.hasPermission(authentication, permission.toString().toLowerCase(), resourceId);
    }


    /* ********************************************** */
    /*              Other Help Methods                */
    /* ********************************************** */

    private boolean isAdmin(Authentication auth) {
        for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
            if (grantedAuth.getAuthority().contains("ADMIN")) {
                return true;
            }
        }
        return false;
    }

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
            } catch (NoSuchFieldException e) {
                logger.error(e);
            }
        }
        return resourceId;
    }
}
