package eu.eosc.observatory.service;

import gr.athenarc.authorization.domain.AuthTriple;

import java.util.List;
import java.util.Set;

public interface PermissionsService {

    Set<String> getPermissions(String userId, String resourceId);

    Set<AuthTriple> addManagers(List<String> users, List<String> resourceIds);

    Set<AuthTriple> addContributors(List<String> users, List<String> resourceIds);

    Set<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds);
}
