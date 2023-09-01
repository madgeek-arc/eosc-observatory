package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserGroup;
import eu.eosc.observatory.dto.GroupMembers;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface UserGroupService {

    Set<User> getMembers(String groupId);

    Set<User> updateMembers(String groupId, Set<String> memberIds);

    Set<User> addMember(String groupId, String memberId);

    Set<User> removeMember(String groupId, String memberId);

    Set<User> getAdmins(String groupId);

    Set<User> updateAdmins(String groupId, Set<String> adminIds);

    Set<User> addAdmin(String groupId, String adminId);

    Set<User> removeAdmin(String groupId, String adminId);

    default GroupMembers<User> getGroupMembers(UserGroup userGroup, CrudService service) {
        Set<?> managers = new HashSet<>();
        Set<?> contributors = new HashSet<>();
        if (userGroup.getAdmins() != null) {
            managers = userGroup.getAdmins()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(service::get)
                    .collect(Collectors.toSet());
        }
        if (userGroup.getMembers() != null) {
            contributors = userGroup.getMembers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(service::get)
                    .collect(Collectors.toSet());
        }

        return new GroupMembers(contributors, managers);
    }
}
