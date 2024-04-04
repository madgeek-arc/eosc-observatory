package eu.eosc.observatory.configuration.security;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface MethodSecurityExpressions {

    // UserGroup Methods

    boolean userIsMemberOfGroup(String userId, String groupId);

    boolean userIsMemberOfGroup(String userId, List<String> groupIds);

    // Stakeholder Methods

    boolean userIsStakeholderMember(String userId, String stakeholderId);

    boolean isStakeholderMember(String stakeholderId);

    boolean userIsStakeholderManager(String userId, String stakeholderId);

    boolean isStakeholderManager(String stakeholderId);

    // Coordinator Methods

    boolean userIsCoordinator(String userId, String coordinatorId);

    boolean isCoordinator(String coordinatorId);

    boolean userIsCoordinatorOfType(String userId, String type);

    boolean isCoordinatorOfType(String type);

    boolean userIsCoordinatorOfStakeholder(String userId, String stakehodlerId);

    boolean isCoordinatorOfStakeholder(String stakehodlerId);

    boolean hasStakeholderManagerAccessOnSurvey(String surveyId);

    boolean hasCoordinatorAccessOnSurvey(String surveyId);

    boolean hasStakeholderManagerAccess(Object surveyAnswer);

    boolean hasCoordinatorAccess(Object surveyAnswer);

    // Administrator Methods
    boolean userIsAdministrator(String userId, String administratorId);

    boolean isAdministrator(String administratorId);

    boolean userIsAdministratorOfType(String userId, String type);

    boolean isAdministratorOfType(String type);

//    boolean userIsAdministratorOfStakeholder(String userId, String stakehodlerId);

//    boolean isAdministratorOfStakeholder(String stakehodlerId);

    // Extra Methods

    boolean hasAccess(Object resource, Object permission);

    boolean isAdmin(Authentication authentication);

}
