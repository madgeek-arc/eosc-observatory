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

import eu.openaire.observatory.domain.Administrator;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.permissions.Groups;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.permissions.Permissions;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static eu.openaire.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

@Service
public class AdministratorServiceImpl extends AbstractUserGroupService<Administrator> implements AdministratorService {

    private static final Logger logger = LoggerFactory.getLogger(AdministratorServiceImpl.class);

    private static final String RESOURCE_TYPE = "administrator";

    private final CrudService<SurveyAnswer> surveyAnswerCrudService;
    private final PermissionService permissionService;

    public AdministratorServiceImpl(ResourceTypeService resourceTypeService,
                                    ResourceService resourceService,
                                    SearchService searchService,
                                    VersionService versionService,
                                    ParserService parserService,
                                    UserService userService,
                                    @Lazy CrudService<SurveyAnswer> surveyAnswerCrudService,
                                    PermissionService permissionService,
                                    ModelResponseValidator validator) {
        super(userService, resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.permissionService = permissionService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String createId(Administrator administrator) {
        String id = String.format("admin-%s", administrator.getType());
        logger.debug("Created new ID for Administrator: {}", id);
        return id;
    }

    @Override
    public Administrator add(Administrator administrator) {
        administrator = super.add(administrator);
        updatePermissions(administrator, Collections.emptySet(), administrator.getMembers());
        updatePermissions(administrator, Collections.emptySet(), administrator.getAdmins());
        return administrator;
    }

    @Override
    public Administrator update(String id, Administrator administrator) {
        Administrator existing = get(id);
        updatePermissions(administrator, existing.getMembers(), administrator.getMembers());
        updatePermissions(administrator, existing.getAdmins(), administrator.getAdmins());
        return super.update(id, administrator);
    }

    @Override
    public SortedSet<String> updateMembers(String administratorId, Set<String> userIds) {
        return super.updateMembers(administratorId, userIds, existing -> {
            updatePermissions((Administrator) existing, existing.getMembers(), userIds);
        });
    }

    @Override
    public SortedSet<String> addMember(String administratorId, String userId) {
        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(userId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(administratorId), administratorId);
        return super.addMember(administratorId, userId);
    }

    @Override
    public SortedSet<String> removeMember(String administratorId, String adminId) {
        // remove Administrator permissions from user
        permissionService.removeAll(adminId, Groups.ADMINISTRATOR.getKey());
        permissionService.removeAll(adminId, administratorId);
        return super.removeMember(administratorId, adminId);
    }

    @Override
    public SortedSet<String> updateAdmins(String administratorId, Set<String> adminIds) {
        return super.updateAdmins(administratorId, adminIds, existing -> {
            updatePermissions((Administrator) existing, existing.getAdmins(), adminIds);
        });
    }

    @Override
    public SortedSet<String> addAdmin(String administratorId, String adminId) {
        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(adminId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(administratorId), administratorId);
        return super.addAdmin(administratorId, adminId);
    }

    @Override
    public SortedSet<String> removeAdmin(String administratorId, String adminId) {
        // remove Administrator permissions from user
        permissionService.removeAll(adminId, Groups.ADMINISTRATOR.getKey());
        permissionService.removeAll(adminId, administratorId);
        return super.removeAdmin(administratorId, adminId);
    }

    private void updatePermissions(Administrator administrator, Set<String> existingUsers, Set<String> userIds) {
        for (String member : userIds) {
            existingUsers.remove(member);
        }

        // remove Administrator permissions from removed members
        permissionService.removeAll(existingUsers, Groups.ADMINISTRATOR.getKey());
        permissionService.removeAll(existingUsers, administrator.getId());

        // read access for all resources
        permissionService.addPermissions(userIds, Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(administrator), administrator.getId());
    }

    private List<String> getAccessibleResourceIds(Administrator administrator) {
        // read access for all resources
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("type", administrator.getType());
        List<SurveyAnswer> resourceIds = surveyAnswerCrudService.getAll(filter).getResults();
        return getSurveyAnswerIds(resourceIds);
    }

    private List<String> getAccessibleResourceIds(String administratorId) {
        return getAccessibleResourceIds(get(administratorId));
    }
}
