package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.service.UserService;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

public class CustomMethodSecurityExpressionRoot
        extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {

    private final UserService userService;

    public CustomMethodSecurityExpressionRoot(Authentication authentication, UserService userService) {
        super(authentication);
        this.userService = userService;
    }

    public boolean isMember(String stakeholderId) {
        User user = userService.get(User.getId(this.authentication));
        // TODO: check if user is contributor or manager of the stakeholder with id 'stakeholderId'
        return false;
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
