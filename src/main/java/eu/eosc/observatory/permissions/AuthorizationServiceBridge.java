package eu.eosc.observatory.permissions;

import eu.eosc.observatory.dto.ResourcePermissions;
import gr.athenarc.authorization.domain.Permission;
import gr.athenarc.authorization.repository.PermissionRepository;
import gr.athenarc.authorization.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthorizationServiceBridge implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceBridge.class);

    private final AuthorizationService authorizationService;
    private final PermissionRepository permissionRepository;

    public AuthorizationServiceBridge(AuthorizationService authorizationService,
                                      PermissionRepository permissionRepository) {
        this.authorizationService = authorizationService;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Set<String> getPermissions(String userId, String resourceId) {
        Set<String> permissions = this.authorizationService.whatCan(userId, resourceId)
                .stream().map(Permission::getAction).collect(Collectors.toSet());
        logger.debug(String.format("[user: %s] permissions for resource [resourceId: %s]: [permissions: %s]", userId, resourceId, String.join(", ", permissions)));
        return permissions;
    }

    @Override
    public Set<Permission> getUserPermissionsByAction(String userId, String action) {
        return this.authorizationService.whereCan(userId, action);
    }

    @Override
    public List<ResourcePermissions> getResourcePermissions(String userId, List<String> resourceIds) {
        List<ResourcePermissions> resourcePermissionsList = new ArrayList<>();
        for (String id : resourceIds) {
            ResourcePermissions resourcePermissions = new ResourcePermissions(id, getPermissions(userId, id));
            resourcePermissionsList.add(resourcePermissions);
        }
        return resourcePermissionsList;
    }

    @Override
    public Set<Permission> addManagers(List<String> users, List<String> resourceIds) {
        List<String> permissions = Arrays.asList(
                Permissions.READ.getKey(),
                Permissions.WRITE.getKey(),
                Permissions.MANAGE.getKey(),
                Permissions.PUBLISH.getKey());
        return addPermissions(users, permissions, resourceIds, Groups.STAKEHOLDER_MANAGER.getKey());
    }

    @Override
    public Set<Permission> addContributors(List<String> users, List<String> resourceIds) {
        List<String> permissions = Arrays.asList(Permissions.READ.getKey(), Permissions.WRITE.getKey());
        return addPermissions(users, permissions, resourceIds, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
    }

    @Override
    public Set<Permission> addPermissions(List<String> users, List<String> actions, List<String> resourceIds, String group) {
        Set<Permission> permissions = new HashSet<>();
        if (users != null && actions != null && resourceIds != null) {
            for (String id : users) {
                for (String action : actions) {
                    for (String resourceId : resourceIds) {
                        if (permissionRepository.findAllBySubjectAndActionAndObject(id, action, resourceId).isEmpty()) {
                            permissions.add(new Permission(id, action, resourceId, group));
                        }
                    }
                }
            }
        }
        permissionRepository.saveAll(permissions);
        return permissions;
    }

    @Override
    public void removePermissions(List<String> users, List<String> actions, List<String> resourceIds) {
        if (users != null && actions != null && resourceIds != null) {
            for (String id : users) {
                for (String action : actions) {
                    for (String resourceId : resourceIds) {
                        Set<Permission> permissions = permissionRepository.findAllBySubjectAndActionAndObject(id, action, resourceId);
                        for (Permission permission : permissions) {
                            logger.debug("Deleting permission: " + permission);
                            permissionRepository.delete(permission);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void removePermissions(List<String> users, List<String> actions, List<String> resourceIds, String group) {
        for (String id : users) {
            for (String action : actions) {
                for (String resourceId : resourceIds) {
                    Set<Permission> permissions = permissionRepository.findAllBySubjectAndActionAndObjectAndSubjectGroup(id, action, resourceId, group);
                    for (Permission permission : permissions) {
                        logger.debug("Deleting permission: " + permission);
                        permissionRepository.delete(permission);
                    }
                }
            }
        }
    }

    @Override
    public void removeAll(String user) {
        permissionRepository.deleteAllBySubject(user);
    }

    @Override
    public void removeAll(String user, String group) {
        permissionRepository.deleteAllBySubjectAndSubjectGroup(user, group);
    }

    @Override
    public void removeAll(List<String> users) {
        for (String user : users) {
            this.removeAll(user);
        }
    }

    @Override
    public void removeAll(List<String> users, String group) {
        for (String user : users) {
            this.removeAll(user, group);
        }
    }

    @Override
    public void remove(String user, String action, String resourceId) {
        permissionRepository.deleteAllBySubjectAndActionAndObject(user, action, resourceId);
    }

    @Override
    public boolean hasPermission(String user, String action, String resourceId) {
        return authorizationService.canDo(user, action, resourceId);
    }

    @Override
    public boolean canRead(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.READ.getKey(), resourceId);
    }

    @Override
    public boolean canWrite(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.WRITE.getKey(), resourceId);
    }

    @Override
    public boolean canManage(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.MANAGE.getKey(), resourceId);
    }

    @Override
    public boolean canPublish(String userId, String resourceId) {
        return authorizationService.canDo(userId, Permissions.PUBLISH.getKey(), resourceId);
    }
}
