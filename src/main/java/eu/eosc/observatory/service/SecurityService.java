package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.permissions.PermissionService;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    Logger logger = LoggerFactory.getLogger(SecurityService.class);
    private final PermissionService permissionService;
    private final UserService userService;

    public SecurityService(PermissionService permissionService, UserService userService) {
        this.permissionService = permissionService;
        this.userService = userService;
    }

    public boolean canRead(Authentication authentication, String resourceId) {
        User user = getUserOrNull(authentication);
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return user != null && hasAcceptedPolicy(user) && permissionService.canRead(user.getId(), resourceId);
    }

    public boolean canWrite(Authentication authentication, String resourceId) {
        User user = getUserOrNull(authentication);
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return user != null && hasAcceptedPolicy(user) && permissionService.canRead(user.getId(), resourceId);
    }

    public boolean canManage(Authentication authentication, String resourceId) {
        User user = getUserOrNull(authentication);
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
        return user != null && hasAcceptedPolicy(user) && permissionService.canManage(user.getId(), resourceId);
    }

    public boolean hasPermission(Authentication authentication, String action, String resourceId) {
        User user = getUserOrNull(authentication);
        return user != null && hasAcceptedPolicy(user) && permissionService.hasPermission(user.getId(), action, resourceId);
    }

    public boolean hasAcceptedPolicy(User user) {
        logger.warn("TODO: Create policy conditions");
        // TODO : check if user has accepted terms
        // TODO: modify "!user.getPoliciesAccepted().isEmpty()" below according to specifications
//        return user.getPoliciesAccepted() != null && !user.getPoliciesAccepted().isEmpty();
        return true;
    }

    /**
     * Searches the db for a user based on the Authentication.
     * @param authentication
     * @return {@link User}
     */
    @Nullable
    User getUserOrNull(Authentication authentication) {
        User user = null;
        try {
            user = userService.get(User.getId(authentication));
        } catch (ResourceNotFoundException ignore) {
            // in case user is not found in db
            // skip
        }
        return user;
    }
}
