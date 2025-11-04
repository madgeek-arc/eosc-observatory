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

import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.dto.GroupMembers;
import gr.uoa.di.madgik.registry.service.*;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractUserGroupService<T extends UserGroup> extends AbstractCrudService<T> implements UserGroupService {

    protected final UserService userService;

    protected AbstractUserGroupService(UserService userService,
                                       ResourceTypeService resourceTypeService,
                                       ResourceService resourceService,
                                       SearchService searchService,
                                       VersionService versionService,
                                       ParserService parserService) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService);
        this.userService = userService;
    }

    @Override
    public GroupMembers<String> getGroupMembers(String groupId) {
        return getGroupMembers((UserGroup) super.get(getResourceType(), groupId));
    }

    @Override
    public Set<String> getMembers(String groupId) {
        return ((UserGroup) super.get(getResourceType(), groupId)).getMembers();
    }

    @Override
    public Set<String> updateMembers(String groupId, Set<String> memberIds) {
        T group = get(groupId);
        group.setMembers(memberIds);
        return update(groupId, group).getMembers();
    }

    protected Set<String> updateMembers(String groupId, Set<String> memberIds, Consumer<UserGroup> setPermissions) {
        T group = get(groupId);
        setPermissions.accept(group);
        group.setMembers(memberIds);
        return update(groupId, group).getMembers();
    }

    @Override
    public Set<String> addMember(String groupId, String memberId) {
        T group = get(groupId);
        if (group.getMembers() == null) {
            group.setMembers(new HashSet<>());
        }
        group.getMembers().add(memberId.toLowerCase());
        return super.update(groupId, group).getMembers();
    }

    @Override
    public Set<String> removeMember(String groupId, String memberId) {
        T group = get(groupId);
        group.getMembers().remove(memberId.toLowerCase());
        return super.update(groupId, group).getMembers();
    }

    @Override
    public Set<String> getAdmins(String groupId) {
        return ((UserGroup) super.get(getResourceType(), groupId)).getAdmins();
    }

    @Override
    public Set<String> updateAdmins(String groupId, Set<String> memberIds) {
        T group = get(groupId);
        group.setAdmins(memberIds);
        return update(groupId, group).getAdmins();
    }

    protected Set<String> updateAdmins(String groupId, Set<String> memberIds, Consumer<UserGroup> setPermissions) {
        T group = get(groupId);
        setPermissions.accept(group);
        group.setAdmins(memberIds);
        return update(groupId, group).getAdmins();
    }

    @Override
    public Set<String> addAdmin(String groupId, String adminId) {
        T group = get(groupId);
        if (group.getAdmins() == null) {
            group.setAdmins(new HashSet<>());
        }
        group.getAdmins().add(adminId.toLowerCase());
        return super.update(groupId, group).getAdmins();
    }

    @Override
    public Set<String> removeAdmin(String groupId, String adminId) {
        T group = get(groupId);
        group.getAdmins().remove(adminId.toLowerCase());
        return super.update(groupId, group).getAdmins();
    }

    protected Set<User> getUsers(Set<String> userIds) {
        return userIds.stream().map(userService::get).collect(Collectors.toSet());
    }
}
