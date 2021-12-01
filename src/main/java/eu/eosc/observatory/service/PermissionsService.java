package eu.eosc.observatory.service;

import gr.athenarc.authorization.domain.AuthTriple;

import java.util.List;

public interface PermissionsService {

    List<String> getPermissions(String userId, String resourceId);

    List<AuthTriple> addManagers(List<String> users, List<String> resourceIds);

    List<AuthTriple> addContributors(List<String> users, List<String> resourceIds);

    List<AuthTriple> addPermissions(List<String> users, List<String> actions, List<String> resourceIds);
}
