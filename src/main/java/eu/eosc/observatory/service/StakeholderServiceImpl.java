package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.StakeholderMembers;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.authorization.domain.Permission;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static eu.eosc.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

@Service
public class StakeholderServiceImpl extends AbstractCrudItemService<Stakeholder> implements StakeholderService {

    private static final Logger logger = LoggerFactory.getLogger(StakeholderServiceImpl.class);

    private static final String RESOURCE_TYPE = "stakeholder";

    private final UserService userService;
    private final SurveyService surveyService;
    private final PermissionService permissionService;

    @Autowired
    public StakeholderServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  ParserService parserService,
                                  UserService userService,
                                  @Lazy SurveyService surveyService,
                                  PermissionService permissionService) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.userService = userService;
        this.permissionService = permissionService;
        this.surveyService = surveyService;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public Stakeholder updateStakeholderAndUserPermissions(String id, Stakeholder resource) throws ResourceNotFoundException {
        updateManagers(id, resource.getManagers());
        updateContributors(id, resource.getContributors());
        return update(id, resource);
    }

    @Override
    public String createId(Stakeholder stakeholder) {
        if (stakeholder.getId() != null) {
            return stakeholder.getId();
        }
        String idSuffix;
        if (Stakeholder.StakeholderType.fromString(stakeholder.getType()) == Stakeholder.StakeholderType.COUNTRY) {
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
    public StakeholderMembers getMembers(String id) {
        Stakeholder stakeholder = this.get(id);
        return getMembers(stakeholder);
    }

    private StakeholderMembers getMembers(Stakeholder stakeholder) {
        Set<User> managers = new HashSet<>();
        Set<User> contributors = new HashSet<>();
        if (stakeholder.getManagers() != null) {
            managers = stakeholder.getManagers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(userService::getUser)
                    .collect(Collectors.toSet());
        }
        if (stakeholder.getContributors() != null) {
            contributors = stakeholder.getContributors()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(userService::getUser)
                    .collect(Collectors.toSet());
        }

        return new StakeholderMembers(contributors, managers);
    }

    @Override
    public Stakeholder updateContributors(String stakeholderId, Set<String> userIds) {
        Stakeholder stakeholder = get(stakeholderId);
        Set<String> previousContributors = stakeholder.getContributors();
        for (String contributor : userIds) {
            previousContributors.remove(contributor);
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

        stakeholder.setContributors(userIds);
        return update(stakeholderId, stakeholder);
    }

    @Override
    public StakeholderMembers addContributor(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getContributors() == null) {
            stakeholder.setContributors(new HashSet<>());
        }
        stakeholder.getContributors().add(userId);
        stakeholder = update(stakeholderId, stakeholder);

        // read access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addContributorPermissions(Collections.singleton(userId), allResourceIds);

        // all contributor permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addContributorFullPermissions(Collections.singleton(userId), resourceIds);

        return getMembers(stakeholder);
    }

    @Override
    public StakeholderMembers removeContributor(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getContributors().remove(userId);
        stakeholder = update(stakeholderId, stakeholder);
        permissionService.removeAll(userId, Groups.STAKEHOLDER_CONTRIBUTOR.getKey());
        return getMembers(stakeholder);
    }

    @Override
    public Stakeholder updateManagers(String stakeholderId, Set<String> userIds) {
        Stakeholder stakeholder = get(stakeholderId);
        Set<String> previousManagers = stakeholder.getManagers();
        for (String manager : userIds) {
            previousManagers.remove(manager);
        }

        permissionService.removeAll(previousManagers, Groups.STAKEHOLDER_MANAGER.getKey());
        stakeholder.setManagers(userIds);


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
    public StakeholderMembers addManager(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getManagers() == null) {
            stakeholder.setManagers(new HashSet<>());
        }
        stakeholder.getManagers().add(userId);
        stakeholder = update(stakeholderId, stakeholder);

        // read/manage/publish access for all resources
        List<SurveyAnswer> answers = surveyService.getAllByStakeholder(stakeholderId);
        List<String> allResourceIds = getSurveyAnswerIds(answers);
        addManagerPermissions(Collections.singleton(userId), allResourceIds);

        // all manager permissions for active resource
        answers = surveyService.getActive(stakeholderId);
        List<String> resourceIds = getSurveyAnswerIds(answers);
        addManagerFullPermissions(Collections.singleton(userId), resourceIds);

        return getMembers(stakeholder);
    }

    @Override
    public StakeholderMembers removeManager(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getManagers().remove(userId);
        stakeholder = update(stakeholderId, stakeholder);
        permissionService.removeAll(userId, Groups.STAKEHOLDER_MANAGER.getKey());
        return getMembers(stakeholder);
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
}
