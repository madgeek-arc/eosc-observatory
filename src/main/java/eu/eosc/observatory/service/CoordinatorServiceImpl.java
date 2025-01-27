/**
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
package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static eu.eosc.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

@Service
public class CoordinatorServiceImpl extends AbstractUserGroupService<Coordinator> implements CoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorServiceImpl.class);

    private static final String RESOURCE_TYPE = "coordinator";

    private final CrudService<SurveyAnswer> surveyAnswerCrudService;
    private final PermissionService permissionService;

    public CoordinatorServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  VersionService versionService,
                                  ParserService parserService,
                                  UserService userService,
                                  @Lazy CrudService<SurveyAnswer> surveyAnswerCrudService,
                                  PermissionService permissionService) {
        super(userService, resourceTypeService, resourceService, searchService, versionService, parserService);
        this.permissionService = permissionService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String createId(Coordinator coordinator) {
        String id = String.format("co-%s", coordinator.getType());
        logger.debug("Created new ID for Coordinator: {}", id);
        return id;
    }

    @Override
    public Coordinator add(Coordinator coordinator) {
        coordinator = super.add(coordinator);
        updatePermissions(coordinator, Collections.emptySet(), coordinator.getMembers());
        updatePermissions(coordinator, Collections.emptySet(), coordinator.getAdmins());
        return coordinator;
    }

    @Override
    public Coordinator update(String id, Coordinator coordinator) {
        Coordinator existing = get(id);
        updatePermissions(coordinator, existing.getMembers(), coordinator.getMembers());
        updatePermissions(coordinator, existing.getAdmins(), coordinator.getAdmins());
        return super.update(id, coordinator);
    }

    @Override
    public Set<String> updateMembers(String coordinatorId, Set<String> userIds) {
        return super.updateMembers(coordinatorId, userIds, existing -> {
            updatePermissions((Coordinator) existing, existing.getMembers(), userIds);
        });
    }

    @Override
    public Set<String> addMember(String coordinatorId, String userId) {
        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(userId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(coordinatorId), coordinatorId);
        return super.addMember(coordinatorId, userId);
    }

    @Override
    public Set<String> removeMember(String coordinatorId, String adminId) {
        // remove Coordinator permissions from user
        permissionService.removeAll(adminId, Groups.COORDINATOR.getKey());
        permissionService.removeAll(adminId, coordinatorId);
        return super.removeMember(coordinatorId, adminId);
    }

    @Override
    public Set<String> updateAdmins(String coordinatorId, Set<String> adminIds) {
        return super.updateAdmins(coordinatorId, adminIds, existing -> {
            updatePermissions((Coordinator) existing, existing.getAdmins(), adminIds);
        });
    }

    @Override
    public Set<String> addAdmin(String coordinatorId, String adminId) {
        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(adminId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(coordinatorId), coordinatorId);
        return super.addAdmin(coordinatorId, adminId);
    }

    @Override
    public Set<String> removeAdmin(String coordinatorId, String adminId) {
        // remove Coordinator permissions from user
        permissionService.removeAll(adminId, Groups.COORDINATOR.getKey());
        permissionService.removeAll(adminId, coordinatorId);
        return super.removeAdmin(coordinatorId, adminId);
    }

    private void updatePermissions(Coordinator coordinator, Set<String> existingUsers, Set<String> userIds) {
        for (String member : userIds) {
            existingUsers.remove(member);
        }

        // remove Coordinator permissions from removed members
        permissionService.removeAll(existingUsers, Groups.COORDINATOR.getKey());
        permissionService.removeAll(existingUsers, coordinator.getId());

        // read access for all resources
        permissionService.addPermissions(userIds, Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(coordinator), coordinator.getId());
    }

    private List<String> getAccessibleResourceIds(Coordinator coordinator) {
        // read access for all resources
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("type", coordinator.getType());
        List<SurveyAnswer> resourceIds = surveyAnswerCrudService.getAll(filter).getResults();
        return getSurveyAnswerIds(resourceIds);
    }

    private List<String> getAccessibleResourceIds(String coordinatorId) {
        return getAccessibleResourceIds(get(coordinatorId));
    }
}
