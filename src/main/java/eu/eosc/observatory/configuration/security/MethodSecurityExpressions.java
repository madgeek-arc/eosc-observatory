package eu.eosc.observatory.configuration.security;

import org.springframework.security.core.Authentication;

import java.util.List;

public interface MethodSecurityExpressions {

    boolean userIsMemberOfGroup(String userId, String groupId);

    boolean userIsMemberOfGroup(String userId, List<String> groupIds);

    boolean userIsStakeholderMember(String userId, String stakeholderId);

    boolean isStakeholderMember(String stakeholderId);

    boolean userIsStakeholderManager(String userId, String stakeholderId);

    boolean isStakeholderManager(String stakeholderId);

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

    boolean hasAccess(Object resource, Object permission);

    boolean isAdmin(Authentication authentication);

}
