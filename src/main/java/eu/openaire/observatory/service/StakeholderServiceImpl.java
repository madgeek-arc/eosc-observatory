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
package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.domain.UserGroup;
import eu.openaire.observatory.dto.UserDTO;
import eu.openaire.observatory.permissions.Groups;
import eu.openaire.observatory.permissions.PermissionService;
import eu.openaire.observatory.permissions.Permissions;
import gr.uoa.di.madgik.authorization.domain.Permission;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static eu.openaire.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

@Service
public class StakeholderServiceImpl extends AbstractUserGroupService<Stakeholder> implements StakeholderService {

    private static final Logger logger = LoggerFactory.getLogger(StakeholderServiceImpl.class);

    private static final String RESOURCE_TYPE = "stakeholder";

    private final SurveyService surveyService;
    private final PermissionService permissionService;

    public StakeholderServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  VersionService versionService,
                                  ParserService parserService,
                                  UserService userService,
                                  @Lazy SurveyService surveyService,
                                  PermissionService permissionService) {
        super(userService, resourceTypeService, resourceService, searchService, versionService, parserService);
        this.permissionService = permissionService;
        this.surveyService = surveyService;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    // Do not remove - used by aspect
    public Stakeholder add(Stakeholder resource) {
        return super.add(resource);
    }

    @Override
    // Do not remove - used by aspect
    public Stakeholder delete(String id) throws ResourceNotFoundException {
        return super.delete(id);
    }

    @Override
    public Stakeholder updateStakeholderAndUserPermissions(String id, Stakeholder resource) throws ResourceNotFoundException {
        updateManagers(id, resource.getAdmins());
        updateContributors(id, resource.getMembers());
        return update(id, resource);
    }

    @Override
    public String createId(Stakeholder stakeholder) {
        if (stakeholder.getId() != null) {
            return stakeholder.getId();
        }
        String idSuffix;
        if (UserGroup.GroupType.fromString(stakeholder.getType()) == UserGroup.GroupType.COUNTRY
                || UserGroup.GroupType.fromString(stakeholder.getType()) == UserGroup.GroupType.EOSC_SB) {
            idSuffix = stakeholder.getCountry();
        } else {
            idSuffix = stakeholder.getName().toLowerCase();
        }
        idSuffix = idSuffix
                .replaceAll("[^a-zA-Z0-9]", " ")
                .trim()
                .replace(' ', '.');
        return String.format("sh-%s-%s", stakeholder.getType(), idSuffix);
    }


    @Override
    public Stakeholder updateContributors(String stakeholderId, Set<String> userIds) {
        Stakeholder stakeholder = get(stakeholderId);
        Set<String> previousContributors = stakeholder.getMembers();

        if (userIds != null && previousContributors != null) {
            for (String contributor : userIds) {
                previousContributors.remove(contributor);
            }
        }

        permissionService.removeAll(previousContributors, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        stakeholder.setMembers(userIds);

        // read access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addContributorPermissions(userIds, allResourceIds);

        // all contributor permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addContributorFullPermissions(userIds, resourceIds);

        return update(stakeholderId, stakeholder);
    }

    @Override
    public Set<String> addMember(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getMembers() == null) {
            stakeholder.setMembers(new HashSet<>());
        }
        stakeholder.getMembers().add(userId);
        stakeholder = update(stakeholderId, stakeholder);

        // read access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addContributorPermissions(Collections.singleton(userId), allResourceIds);

        // all contributor permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addContributorFullPermissions(Collections.singleton(userId), resourceIds);

        return stakeholder.getMembers();
    }

    @Override
    public Set<String> removeMember(String stakeholderId, String memberId) {
        permissionService.removeAll(memberId, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        return super.removeMember(stakeholderId, memberId);
    }

    @Override
    public Stakeholder updateManagers(String stakeholderId, Set<String> userIds) {
        Stakeholder stakeholder = get(stakeholderId);
        Set<String> previousManagers = stakeholder.getAdmins();

        if (userIds != null && previousManagers != null) {
            for (String manager : userIds) {
                previousManagers.remove(manager);
            }
        }

        permissionService.removeAll(previousManagers, Groups.STAKEHOLDER_MANAGER.getKey());
        stakeholder.setAdmins(userIds);

        // read/manage/publish access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addManagerPermissions(userIds, allResourceIds);

        // all manager permissions for active resources
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addManagerFullPermissions(userIds, resourceIds);

        return update(stakeholderId, stakeholder);
    }

    @Override
    public List<UserDTO> getManagers(String stakeholderId) {
        return this.get(stakeholderId)
                .getAdmins()
                .stream()
                .map(userService::getUser)
                .map(UserDTO::new)
                .toList();
    }

    @Override
    public Set<String> addAdmin(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getAdmins() == null) {
            stakeholder.setAdmins(new HashSet<>());
        }
        stakeholder.getAdmins().add(userId);
        stakeholder = update(stakeholderId, stakeholder);

        // read/manage/publish access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addManagerPermissions(Collections.singleton(userId), allResourceIds);

        // all manager permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addManagerFullPermissions(Collections.singleton(userId), resourceIds);

        return stakeholder.getAdmins();
    }

    @Override
    public Set<String> removeAdmin(String stakeholderId, String adminId) {
        permissionService.removeAll(adminId, Groups.STAKEHOLDER_MANAGER.getKey());
        return super.removeAdmin(stakeholderId, adminId);
    }

    private Set<Permission> addManagerFullPermissions(Set<String> users, List<String> resourceIds) {
        List<String> permissions = List.of(
                Permissions.READ.getKey(),
                Permissions.WRITE.getKey(),
                Permissions.MANAGE.getKey(),
                Permissions.PUBLISH.getKey());
        return permissionService.addPermissions(users, permissions, resourceIds, Groups.STAKEHOLDER_MANAGER.getKey());
    }

    private Set<Permission> addManagerPermissions(Set<String> users, List<String> resourceIds) {
        List<String> permissions = List.of(
                Permissions.READ.getKey(),
                Permissions.MANAGE.getKey(),
                Permissions.PUBLISH.getKey());
        return permissionService.addPermissions(users, permissions, resourceIds, Groups.STAKEHOLDER_MANAGER.getKey());
    }

    private Set<Permission> addContributorFullPermissions(Set<String> users, List<String> resourceIds) {
        List<String> permissions = List.of(Permissions.READ.getKey(), Permissions.WRITE.getKey());
        return permissionService.addPermissions(users, permissions, resourceIds, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
    }

    private Set<Permission> addContributorPermissions(Set<String> users, List<String> resourceIds) {
        List<String> permissions = List.of(Permissions.READ.getKey());
        return permissionService.addPermissions(users, permissions, resourceIds, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
    }

    @Override
    public Set<String> updateMembers(String groupId, Set<String> memberIds) {
        return updateContributors(groupId, memberIds).getMembers();
    }

    @Override
    public Set<String> updateAdmins(String groupId, Set<String> adminIds) {
        return updateManagers(groupId, adminIds).getAdmins();
    }
}
