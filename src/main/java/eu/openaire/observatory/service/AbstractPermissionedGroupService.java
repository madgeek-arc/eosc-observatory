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

import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.permissions.Groups;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.permissions.Permissions;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.context.annotation.Lazy;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static eu.openaire.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

public abstract class AbstractPermissionedGroupService<T extends UserGroup> extends AbstractUserGroupService<T> {

    protected final CrudService<SurveyAnswer> surveyAnswerCrudService;
    protected final PermissionService permissionService;

    protected AbstractPermissionedGroupService(UserService userService,
                                               ResourceTypeService resourceTypeService,
                                               ResourceService resourceService,
                                               SearchService searchService,
                                               VersionService versionService,
                                               ParserService parserService,
                                               @Lazy CrudService<SurveyAnswer> surveyAnswerCrudService,
                                               PermissionService permissionService,
                                               ModelResponseValidator validator) {
        super(userService, resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.permissionService = permissionService;
    }

    protected abstract Groups getGroup();

    @Override
    public T add(T entity) {
        entity = super.add(entity);
        updatePermissions(entity, Collections.emptySet(), entity.getMembers());
        updatePermissions(entity, Collections.emptySet(), entity.getAdmins());
        return entity;
    }

    @Override
    public T update(String id, T entity) {
        T existing = get(id);
        updatePermissions(entity, existing.getMembers(), entity.getMembers());
        updatePermissions(entity, existing.getAdmins(), entity.getAdmins());
        return super.update(id, entity);
    }

    @Override
    public SortedSet<String> updateMembers(String groupId, Set<String> userIds) {
        return super.updateMembers(groupId, userIds, existing -> {
            updatePermissions((T) existing, existing.getMembers(), userIds);
        });
    }

    @Override
    public SortedSet<String> addMember(String groupId, String userId) {
        permissionService.addPermissions(Collections.singletonList(userId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(groupId), groupId);
        return super.addMember(groupId, userId);
    }

    @Override
    public SortedSet<String> removeMember(String groupId, String memberId) {
        permissionService.removeAll(memberId, getGroup().getKey());
        permissionService.removeAll(memberId, groupId);
        return super.removeMember(groupId, memberId);
    }

    @Override
    public SortedSet<String> updateAdmins(String groupId, Set<String> adminIds) {
        return super.updateAdmins(groupId, adminIds, existing -> {
            updatePermissions((T) existing, existing.getAdmins(), adminIds);
        });
    }

    @Override
    public SortedSet<String> addAdmin(String groupId, String adminId) {
        permissionService.addPermissions(Collections.singletonList(adminId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(groupId), groupId);
        return super.addAdmin(groupId, adminId);
    }

    @Override
    public SortedSet<String> removeAdmin(String groupId, String adminId) {
        permissionService.removeAll(adminId, getGroup().getKey());
        permissionService.removeAll(adminId, groupId);
        return super.removeAdmin(groupId, adminId);
    }

    private void updatePermissions(T entity, Set<String> existingUsers, Set<String> userIds) {
        for (String member : userIds) {
            existingUsers.remove(member);
        }
        permissionService.removeAll(existingUsers, getGroup().getKey());
        permissionService.removeAll(existingUsers, entity.getId());
        permissionService.addPermissions(userIds, Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(entity), entity.getId());
    }

    private List<String> getAccessibleResourceIds(T entity) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("type", entity.getType());
        List<SurveyAnswer> resourceIds = surveyAnswerCrudService.getAll(filter).getResults();
        return getSurveyAnswerIds(resourceIds);
    }

    private List<String> getAccessibleResourceIds(String groupId) {
        return getAccessibleResourceIds(get(groupId));
    }
}
