package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.UserGroup;
import eu.eosc.observatory.dto.GroupMembers;

import java.util.Set;

public interface UserGroupService {

    GroupMembers<String> getGroupMembers(String groupId);

    default GroupMembers<String> getGroupMembers(UserGroup group) {
        return new GroupMembers<>(group.getMembers(), group.getAdmins());
    }

    Set<String> getMembers(String groupId);

    Set<String> updateMembers(String groupId, Set<String> memberIds);

    Set<String> addMember(String groupId, String memberId);

    Set<String> removeMember(String groupId, String memberId);

    Set<String> getAdmins(String groupId);

    Set<String> updateAdmins(String groupId, Set<String> adminIds);

    Set<String> addAdmin(String groupId, String adminId);

    Set<String> removeAdmin(String groupId, String adminId);
}
