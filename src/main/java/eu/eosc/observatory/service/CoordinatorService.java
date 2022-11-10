package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.User;

import java.util.Set;

public interface CoordinatorService extends CrudItemService<Coordinator> {

    Set<User> getMembers(String id);

    Set<User> updateMembers(String coordinatorId, Set<String> userIds);

    Set<User> addMember(String coordinatorId, String userId);

    Set<User> removeMember(String coordinatorId, String userId);
}
