package eu.eosc.observatory.permissions;

import eu.eosc.observatory.dto.ResourcePermissions;
import gr.athenarc.authorization.domain.Permission;

import java.util.Collection;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissions(String userId, String resourceId);

    Set<Permission> getUserPermissionsByAction(String userId, String action);

    Collection<ResourcePermissions> getResourcePermissions(String userId, Collection<String> resourceIds);

    Set<Permission> addPermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds, String group);

    void removePermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds);

    void removePermissions(Collection<String> users, Collection<String> actions, Collection<String> resourceIds, String group);

    @Deprecated
    Set<Permission> addManagers(Collection<String> users, Collection<String> resourceIds);

    @Deprecated
    Set<Permission> addContributors(Collection<String> users, Collection<String> resourceIds);

    void removeAll(String user);

    void removeAll(String user, String group);

    void removeAll(Collection<String> users);

    void removeAll(Collection<String> users, String group);

    void remove(String user, String action, String resourceId);

    boolean hasPermission(String user, String action, String resourceId);

    boolean canRead(String userId, String resourceId);

    boolean canWrite(String userId, String resourceId);

    boolean canManage(String userId, String resourceId);

    boolean canPublish(String userId, String resourceId);
}
