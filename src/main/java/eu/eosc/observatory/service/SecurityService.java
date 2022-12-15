package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.permissions.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public SecurityService(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    public boolean canRead(Authentication authentication, String resourceId) {
        User user = userService.get(User.getId(authentication));
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return !user.getPoliciesAccepted().isEmpty() && permissionService.canRead(user.getId(), resourceId);
    }

    public boolean canWrite(Authentication authentication, String resourceId) {
        User user = userService.get(User.getId(authentication));
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return !user.getPoliciesAccepted().isEmpty() && permissionService.canRead(user.getId(), resourceId);
    }

    public boolean canManage(Authentication authentication, String resourceId) {
        User user = userService.get(User.getId(authentication));
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return !user.getPoliciesAccepted().isEmpty() && permissionService.canManage(user.getId(), resourceId);
    }

    public boolean hasPermission(Authentication authentication, String action, String resourceId) {
        logger.debug("hasPermission(auth, action, resourceId)\nAuthentication: {}\nAction: {}\nResourceId: {}", authentication, action, resourceId);
        User user = userService.get(User.getId(authentication));
        // TODO : check if user has accepted terms
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return !user.getPoliciesAccepted().isEmpty() && permissionService.hasPermission(user.getId(), action, resourceId);
    }
}
