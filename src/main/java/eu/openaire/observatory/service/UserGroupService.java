/*
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.dto.GroupMembers;

import java.util.Set;
import java.util.SortedSet;

public interface UserGroupService {

    GroupMembers<String> getGroupMembers(String groupId);

    default GroupMembers<String> getGroupMembers(UserGroup group) {
        return new GroupMembers<>(group.getMembers().stream().toList(), group.getAdmins().stream().toList());
    }

    SortedSet<String> getMembers(String groupId);

    SortedSet<String> updateMembers(String groupId, Set<String> memberIds);

    SortedSet<String> addMember(String groupId, String memberId);

    SortedSet<String> removeMember(String groupId, String memberId);

    SortedSet<String> getAdmins(String groupId);

    SortedSet<String> updateAdmins(String groupId, Set<String> adminIds);

    SortedSet<String> addAdmin(String groupId, String adminId);

    SortedSet<String> removeAdmin(String groupId, String adminId);
}
