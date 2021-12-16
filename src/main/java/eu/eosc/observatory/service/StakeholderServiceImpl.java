package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.dto.StakeholderMembers;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StakeholderServiceImpl extends AbstractCrudItemService<Stakeholder> implements StakeholderService {

    private static final Logger logger = LogManager.getLogger(StakeholderServiceImpl.class);

    private static final String RESOURCE_TYPE = "stakeholder";

    private final CrudItemService<User> userService;
    private final SurveyService surveyService;
    private final PermissionService permissionService;

    @Autowired
    public StakeholderServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  ParserService parserService,
                                  CrudItemService<User> userService,
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
    public String createId(Stakeholder stakeholder) {
        String idSuffix;
        if (Stakeholder.StakeholderType.fromString(stakeholder.getType()) == Stakeholder.StakeholderType.COUNTRY) {
            idSuffix = stakeholder.getCountry();
        } else {
            idSuffix = stakeholder.getAssociationMember();
        }
        return String.format("sh-%s-%s", stakeholder.getType(), idSuffix);
    }

    @Override
    public StakeholderMembers getMembers(String id) {
        Stakeholder stakeholder = this.get(id);
        return getMembers(stakeholder);
    }

    private StakeholderMembers getMembers(Stakeholder stakeholder) {
        List<User> managers = new ArrayList<>();
        List<User> contributors = new ArrayList<>();
        if (stakeholder.getManagers() != null) {
            managers = stakeholder.getManagers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::getUser)
                    .collect(Collectors.toList());
        }
        if (stakeholder.getContributors() != null) {
            contributors = stakeholder.getContributors()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::getUser)
                    .collect(Collectors.toList());
        }

        return new StakeholderMembers(contributors, managers);
    }

    @Override
    public Stakeholder updateContributors(String stakeholderId, List<String> userIds) {
        Stakeholder stakeholder = get(stakeholderId);
        List<String> previousContributors = stakeholder.getManagers();
        for (String manager : userIds) {
            previousContributors.remove(manager);
        }
        stakeholder.setContributors(userIds);
        permissionService.removeAll(previousContributors);
        return update(stakeholderId, stakeholder);
    }

    @Override
    public StakeholderMembers addContributor(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getContributors() == null) {
            stakeholder.setContributors(new ArrayList<>());
        }
        stakeholder.getContributors().add(userId);
        stakeholder = update(stakeholderId, stakeholder);

        // read access for all resources
        List<String> allResourceIds = surveyService.getAllByStakeholder(stakeholderId).stream().map(SurveyAnswer::getId).collect(Collectors.toList());
        permissionService.addPermissions(Collections.singletonList(userId), Collections.singletonList(PermissionService.Permissions.READ.getKey()), allResourceIds);

        // all contributor permissions for active resource
        List<String> activeResourceIds = surveyService.getActive(stakeholderId).stream().map(SurveyAnswer::getId).collect(Collectors.toList());
        permissionService.addContributors(Collections.singletonList(userId), activeResourceIds);
        return getMembers(stakeholder);
    }

    @Override
    public StakeholderMembers removeContributor(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getContributors().remove(userId);
        stakeholder = update(stakeholderId, stakeholder);
        permissionService.removeAll(userId);
        return getMembers(stakeholder);
    }

    @Override
    public Stakeholder updateManagers(String stakeholderId, List<String> userIds) {
        Stakeholder stakeholder = get(stakeholderId);
        List<String> previousManagers = stakeholder.getManagers();
        for (String manager : userIds) {
            previousManagers.remove(manager);
        }
        permissionService.removeAll(previousManagers);
        stakeholder.setManagers(userIds);
        stakeholder.getManagers().forEach(id -> addManager(stakeholderId, id));
        return update(stakeholderId, stakeholder);
    }

    @Override
    public StakeholderMembers addManager(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        if (stakeholder.getManagers() == null) {
            stakeholder.setManagers(new ArrayList<>());
        }
        stakeholder.getManagers().add(userId);
        stakeholder = update(stakeholderId, stakeholder);

        // read/manage/publish access for all resources
        List<String> permissions = Arrays.asList(
                PermissionService.Permissions.READ.getKey(),
                PermissionService.Permissions.MANAGE.getKey(),
                PermissionService.Permissions.PUBLISH.getKey());
        List<String> allResourceIds = surveyService.getAllByStakeholder(stakeholderId).stream().map(SurveyAnswer::getId).collect(Collectors.toList());
        permissionService.addPermissions(Collections.singletonList(userId), permissions, allResourceIds);

        // all manager permissions for active resource
        List<String> resourceIds = surveyService.getActive(stakeholderId).stream().map(SurveyAnswer::getId).collect(Collectors.toList());
        permissionService.addManagers(Collections.singletonList(userId), resourceIds);
        return getMembers(stakeholder);
    }

    @Override
    public StakeholderMembers removeManager(String stakeholderId, String userId) {
        Stakeholder stakeholder = get(stakeholderId);
        stakeholder.getManagers().remove(userId);
        stakeholder = update(stakeholderId, stakeholder);
        permissionService.removeAll(userId);
        return getMembers(stakeholder);
    }

    private User getUser(String email) {
        User user = null;
        try {
            user = userService.get(email);
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = new User();
            user.setEmail(email);
        }
        return user;
    }
}
