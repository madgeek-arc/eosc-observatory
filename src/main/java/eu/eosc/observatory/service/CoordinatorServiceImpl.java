package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Coordinator;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.service.id.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static eu.eosc.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

@Service
public class CoordinatorServiceImpl extends AbstractCrudItemService<Coordinator> implements CoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(CoordinatorServiceImpl.class);

    private static final String RESOURCE_TYPE = "coordinator";

    private final UserService userService;
    private final CrudItemService<SurveyAnswer> surveyAnswerCrudService;
    private final PermissionService permissionService;
    private final IdGenerator<String> idGenerator;

    @Autowired
    public CoordinatorServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  ParserService parserService,
                                  UserService userService,
                                  @Lazy CrudItemService<SurveyAnswer> surveyAnswerCrudService,
                                  PermissionService permissionService,
                                  IdGenerator<String> idGenerator) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.userService = userService;
        this.permissionService = permissionService;
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.idGenerator = idGenerator;
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String createId(Coordinator coordinator) {
        String prefix = String.format("co-%s-", coordinator.getType());
        String id = idGenerator.createId(prefix);
        logger.debug(String.format("Created new ID for Coordinator: %s", id));
        return id;
    }

    @Override
    public Coordinator add(Coordinator coordinator) {
        Coordinator newCoordinator = super.add(coordinator);
        updateRoles(newCoordinator.getId(), coordinator.getMembers());
        return newCoordinator;
    }

    @Override
    public Coordinator update(String id, Coordinator coordinator) {
        updateRoles(id, coordinator.getMembers());
        return super.update(id, coordinator);
    }

    @Override
    public Set<User> getMembers(String id) {
        Coordinator coordinator = get(id);
        return getMembers(coordinator);
    }

    @Override
    public Set<User> updateMembers(String coordinatorId, List<String> userIds) {
        Coordinator coordinator = updateRoles(coordinatorId, userIds);

        coordinator = super.update(coordinatorId, coordinator);
        return getMembers(coordinator);
    }

    @Override
    public Set<User> addMember(String coordinatorId, String userId) {
        Coordinator coordinator = get(coordinatorId);
        if (coordinator.getMembers() == null) {
            coordinator.setMembers(new ArrayList<>());
        }
        coordinator.getMembers().add(userId);
        coordinator = super.update(coordinatorId, coordinator);

        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(userId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(coordinator), coordinatorId);

        return getMembers(coordinator);
    }

    @Override
    public Set<User> removeMember(String coordinatorId, String userId) {
        Coordinator coordinator = get(coordinatorId);
        coordinator.getMembers().remove(userId);
        coordinator = super.update(coordinatorId, coordinator);

        // remove Coordinator permissions from user
        permissionService.removeAll(userId, Groups.COORDINATOR.getKey());
        permissionService.removeAll(userId, coordinatorId);
        return getMembers(coordinator);
    }


    private Set<User> getMembers(Coordinator coordinator) {
        Set<User> members = new HashSet<>();
        if (coordinator.getMembers() != null) {
            members = coordinator.getMembers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(userService::getUser)
                    .collect(Collectors.toSet());
        }
        return members;
    }

    private Coordinator updateRoles(String coordinatorId, List<String> userIds) {
        Coordinator coordinator = get(coordinatorId);
        List<String> previousMembers = coordinator.getMembers();
        for (String member : userIds) {
            previousMembers.remove(member);
        }

        // remove Coordinator permissions from removed members
        permissionService.removeAll(previousMembers, Groups.COORDINATOR.getKey());
        permissionService.removeAll(previousMembers, coordinatorId);

        // read access for all resources
        permissionService.addPermissions(userIds, Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(coordinator), coordinatorId);

        coordinator.setMembers(userIds);
        return coordinator;
    }

    private List<String> getAccessibleResourceIds(Coordinator coordinator) {
        // read access for all resources
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);
        filter.addFilter("type", coordinator.getType());
        List<SurveyAnswer> resourceIds = surveyAnswerCrudService.getAll(filter).getResults();
        return getSurveyAnswerIds(resourceIds);
    }
}
