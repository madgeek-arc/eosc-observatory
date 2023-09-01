package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserGroup;
import eu.eosc.observatory.dto.GroupMembers;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.service.*;
import gr.athenarc.authorization.domain.Permission;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;

import static eu.eosc.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

@Service
public class StakeholderServiceImpl extends AbstractCrudService<Stakeholder> implements StakeholderService {

    private static final Logger logger = LoggerFactory.getLogger(StakeholderServiceImpl.class);

    private static final String RESOURCE_TYPE = "stakeholder";

    private final UserService userService;
    private final SurveyService surveyService;
    private final PermissionService permissionService;

    @Autowired
    public StakeholderServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  VersionService versionService,
                                  ParserService parserService,
                                  UserService userService,
                                  @Lazy SurveyService surveyService,
                                  PermissionService permissionService) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService);
        this.userService = userService;
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
        } else if (stakeholder.getAssociationMember() != null) {
            idSuffix = stakeholder.getAssociationMember().toLowerCase();
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
    public GroupMembers<User> getAllMembers(String id) {
        Stakeholder stakeholder = this.get(id);
        return getGroupMembers(stakeholder, userService);
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

        // read access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addContributorPermissions(userIds, allResourceIds);

        // all contributor permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addContributorFullPermissions(userIds, resourceIds);

        stakeholder.setMembers(userIds);
        return update(stakeholderId, stakeholder);
    }

    @Override
    public GroupMembers addContributor(String stakeholderId, String userId) {
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

        return getGroupMembers(stakeholder, userService);
    }

    @Override
    public GroupMembers removeContributor(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getMembers().remove(userId);
        stakeholder = update(stakeholderId, stakeholder);
        permissionService.removeAll(userId, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        return getGroupMembers(stakeholder, userService);
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

        // all manager permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addManagerFullPermissions(userIds, resourceIds);

        return update(stakeholderId, stakeholder);
    }

    @Override
    public GroupMembers addManager(String stakeholderId, String userId) {
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

        return getGroupMembers(stakeholder, userService);
    }

    @Override
    public GroupMembers removeManager(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getAdmins().remove(userId);
        stakeholder = update(stakeholderId, stakeholder);
        permissionService.removeAll(userId, Groups.STAKEHOLDER_MANAGER.getKey());
        return getGroupMembers(stakeholder, userService);
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
    public Set<User> getMembers(String groupId) {
        return getAllMembers(groupId).getMembers();
    }

    @Override
    public Set<User> updateMembers(String groupId, Set<String> memberIds) {
        return getGroupMembers(updateContributors(groupId, memberIds), userService).getAdmins();
    }

    @Override
    public Set<User> addMember(String groupId, String memberId) {
        return addContributor(groupId, memberId).getMembers();
    }

    @Override
    public Set<User> removeMember(String groupId, String memberId) {
        return removeContributor(groupId, memberId).getMembers();
    }

    @Override
    public Set<User> getAdmins(String groupId) {
        return getAllMembers(groupId).getAdmins();
    }

    @Override
    public Set<User> updateAdmins(String groupId, Set<String> adminIds) {
        return getGroupMembers(updateManagers(groupId, adminIds), userService).getAdmins();
    }

    @Override
    public Set<User> addAdmin(String groupId, String adminId) {
        return addManager(groupId, adminId).getAdmins();
    }

    @Override
    public Set<User> removeAdmin(String groupId, String adminId) {
        return removeManager(groupId, adminId).getAdmins();
    }
}
