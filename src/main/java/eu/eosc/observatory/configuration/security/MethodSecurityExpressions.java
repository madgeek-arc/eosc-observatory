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

    boolean userIsCoordinatorMember(String userId, String coordinatorId);

    boolean isCoordinatorMember(String coordinatorId);

    boolean userIsCoordinatorMemberOfType(String userId, String type);

    boolean isCoordinatorMemberOfType(String type);

    boolean userIsCoordinatorMemberOfStakeholder(String userId, String stakehodlerId);

    boolean isCoordinatorMemberOfStakeholder(String stakehodlerId);

    boolean hasStakeholderManagerAccess(Object surveyAnswer);

    boolean hasStakeholderManagerAccess(String surveyId);

    boolean hasCoordinatorAccess(String surveyId);

    boolean hasCoordinatorAccess(Object surveyAnswer);

    boolean hasAccess(Object resource, Object permission);

    boolean isAdmin(Authentication authentication);

}
