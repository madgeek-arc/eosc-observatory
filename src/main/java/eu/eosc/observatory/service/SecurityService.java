package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    private final PermissionsService permissionsService;
    private final UserService userService;

    @Autowired
    public SecurityService(PermissionsService permissionsService, UserService userService) {
        this.permissionsService = permissionsService;
        this.userService = userService;
    }

    public boolean canRead(Authentication authentication, String resourceId) {
        User user = userService.get(User.getId(authentication));
        return user.getConsent() && permissionsService.canRead(user.getId(), resourceId);
    }

    public boolean canWrite(Authentication authentication, String resourceId) {
        User user = userService.get(User.getId(authentication));
        return user.getConsent() && permissionsService.canRead(user.getId(), resourceId);
    }

    public boolean canManage(Authentication authentication, String resourceId) {
        User user = userService.get(User.getId(authentication));
        return user.getConsent() && permissionsService.canManage(user.getId(), resourceId);
    }

    public boolean hasPermission(Authentication authentication, String action, String resourceId) {
        User user = userService.get(User.getId(authentication));
        return user.getConsent() && permissionsService.hasPermission(user.getId(), action, resourceId);
    }
}
