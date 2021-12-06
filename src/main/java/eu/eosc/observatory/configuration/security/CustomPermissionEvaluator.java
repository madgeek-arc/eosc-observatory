package eu.eosc.observatory.configuration.security;

import eu.eosc.observatory.service.Identifiable;
import eu.eosc.observatory.service.SecurityService;
import gr.athenarc.catalogue.ReflectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private static final Logger logger = LogManager.getLogger(CustomPermissionEvaluator.class);

    @Autowired
    private SecurityService securityService;


    @Override
    public boolean hasPermission(Authentication authentication, Object resource, Object permission) {
        if ((authentication == null) || (resource == null) || !(permission instanceof String)) {
            return false;
        }

        // convert to Object when resource is a returnObject (@PostConstruct)
        if (resource instanceof ResponseEntity) {
            resource = ((ResponseEntity) resource).getBody();
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

    @Override
    public boolean hasPermission(Authentication authentication, Serializable serializable, String s, Object o) {
        throw new UnsupportedOperationException("Not Implemented");
    }

    private boolean isAdmin(Authentication auth) {
        for (GrantedAuthority grantedAuth : auth.getAuthorities()) {
            if (grantedAuth.getAuthority().contains("ADMIN")) {
                return true;
            }
        }
        return false;
    }
}
