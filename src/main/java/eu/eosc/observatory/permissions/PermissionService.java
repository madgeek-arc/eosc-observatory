package eu.eosc.observatory.permissions;

import gr.athenarc.authorization.domain.AuthTriple;

import java.util.List;
import java.util.Set;

public interface PermissionService {

    Set<String> getPermissions(String userId, String resourceId);

    Set<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds);

    void removePermissions(List<String> users, List<String> actions, List<String> resourceIds);

    Set<AuthTriple> addManagers(List<String> users, List<String> resourceIds);

    Set<AuthTriple> addContributors(List<String> users, List<String> resourceIds);

    void removeAll(String user);

    void removeAll(List<String> users);

    void remove(String user, String action, String resourceId);

    boolean hasPermission(String user, String action, String resourceId);

    boolean canRead(String userId, String resourceId);

    boolean canWrite(String userId, String resourceId);

    boolean canManage(String userId, String resourceId);

    boolean canPublish(String userId, String resourceId);
}
