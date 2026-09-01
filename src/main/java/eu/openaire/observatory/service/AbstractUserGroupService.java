/*
 * Copyright 2021-2026 OpenAIRE AMKE
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
import eu.openaire.observatory.utils.UserIds;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.service.*;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractUserGroupService<T extends UserGroup> extends AbstractCrudService<T> implements UserGroupService {

    protected final UserService userService;

    protected AbstractUserGroupService(UserService userService,
                                       ResourceTypeService resourceTypeService,
                                       ResourceService resourceService,
                                       SearchService searchService,
                                       VersionService versionService,
                                       ParserService parserService,
                                       ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.userService = userService;
    }

    @Override
    public GroupMembers<String> getGroupMembers(String groupId) {
        return getGroupMembers((UserGroup) super.get(getResourceType(), groupId));
    }

    @Override
    public SortedSet<String> getMembers(String groupId) {
        return ((UserGroup) super.get(getResourceType(), groupId)).getMembers();
    }

    @Override
    public SortedSet<String> updateMembers(String groupId, Set<String> memberIds) {
        T group = get(groupId);
        group.setMembers(new TreeSet<>(memberIds));
        return update(groupId, group).getMembers();
    }

    protected SortedSet<String> updateMembers(String groupId, Set<String> memberIds, Consumer<UserGroup> setPermissions) {
        T group = get(groupId);
        setPermissions.accept(group);
        group.setMembers(new TreeSet<>(memberIds));
        return update(groupId, group).getMembers();
    }

    @Override
    public SortedSet<String> addMember(String groupId, String memberId) {
        T group = get(groupId);
        // Assign through the setter rather than mutating getMembers(): when the underlying field is
        // unset the getter hands back a fresh empty set, so the addition would land on a throwaway
        // collection and be silently lost. The setter also normalizes every id it stores.
        SortedSet<String> members = new TreeSet<>(group.getMembers());
        members.add(UserIds.normalize(memberId));
        group.setMembers(members);
        return super.update(groupId, group).getMembers();
    }

    @Override
    public SortedSet<String> removeMember(String groupId, String memberId) {
        T group = get(groupId);
        group.getMembers().remove(UserIds.normalize(memberId));
        return super.update(groupId, group).getMembers();
    }

    @Override
    public SortedSet<String> getAdmins(String groupId) {
        return ((UserGroup) super.get(getResourceType(), groupId)).getAdmins();
    }

    @Override
    public SortedSet<String> updateAdmins(String groupId, Set<String> memberIds) {
        T group = get(groupId);
        group.setAdmins(new TreeSet<>(memberIds));
        return update(groupId, group).getAdmins();
    }

    protected SortedSet<String> updateAdmins(String groupId, Set<String> memberIds, Consumer<UserGroup> setPermissions) {
        T group = get(groupId);
        setPermissions.accept(group);
        group.setAdmins(new TreeSet<>(memberIds));
        return update(groupId, group).getAdmins();
    }

    @Override
    public SortedSet<String> addAdmin(String groupId, String adminId) {
        T group = get(groupId);
        // See addMember: mutating getAdmins() directly loses the addition when the field is unset.
        SortedSet<String> admins = new TreeSet<>(group.getAdmins());
        admins.add(UserIds.normalize(adminId));
        group.setAdmins(admins);
        return super.update(groupId, group).getAdmins();
    }

    @Override
    public SortedSet<String> removeAdmin(String groupId, String adminId) {
        T group = get(groupId);
        group.getAdmins().remove(UserIds.normalize(adminId));
        return super.update(groupId, group).getAdmins();
    }

    protected SortedSet<User> getUsers(Set<String> userIds) {
        return userIds.stream().map(userService::get).collect(Collectors.toCollection(TreeSet::new));
    }
}
