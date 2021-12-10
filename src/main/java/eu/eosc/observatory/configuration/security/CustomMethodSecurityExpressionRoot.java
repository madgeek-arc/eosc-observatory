package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.StakeholderService;
import eu.eosc.observatory.service.UserService;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class CustomMethodSecurityExpressionRoot
        extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final UserService userService;
    private final StakeholderService stakeholderService;

    public CustomMethodSecurityExpressionRoot(Authentication authentication, UserService userService, StakeholderService stakeholderService) {
        super(authentication);
        this.userService = userService;
        this.stakeholderService = stakeholderService;
    }

    public boolean isStakeholderMember(String stakeholderId) {
        User user = userService.get(User.getId(this.authentication));
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        return stakeholder.getContributors().contains(user.getId()) || stakeholder.getManagers().contains(user.getId());
    }

    public boolean isStakeholderManager(String stakeholderId) {
        User user = userService.get(User.getId(this.authentication));
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        return stakeholder.getContributors().contains(user.getId()) || stakeholder.getManagers().contains(user.getId());
    }

    @Override
    public void setFilterObject(Object o) {

    }

    @Override
    public Object getFilterObject() {
        return null;
    }

    @Override
    public void setReturnObject(Object o) {

    }

    @Override
    public Object getReturnObject() {
        return null;
    }

    @Override
    public Object getThis() {
        return null;
    }
}
