package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Administrator;
import eu.eosc.observatory.domain.Administrator;
import eu.eosc.observatory.domain.SurveyAnswer;
import eu.eosc.observatory.permissions.Groups;
import eu.eosc.observatory.permissions.PermissionService;
import eu.eosc.observatory.permissions.Permissions;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static eu.eosc.observatory.utils.SurveyAnswerUtils.getSurveyAnswerIds;

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
    public Set<String> updateMembers(String administratorId, Set<String> userIds) {
        return super.updateMembers(administratorId, userIds, existing -> {
            updatePermissions((Administrator) existing, existing.getMembers(), userIds);
        });
    }

    @Override
    public Set<String> addMember(String administratorId, String userId) {
        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(userId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(administratorId), administratorId);
        return super.addMember(administratorId, userId);
    }

    @Override
    public Set<String> removeMember(String administratorId, String adminId) {
        // remove Administrator permissions from user
        permissionService.removeAll(adminId, Groups.ADMINISTRATOR.getKey());
        permissionService.removeAll(adminId, administratorId);
        return super.removeMember(administratorId, adminId);
    }

    @Override
    public Set<String> updateAdmins(String administratorId, Set<String> adminIds) {
        return super.updateAdmins(administratorId, adminIds, existing -> {
            updatePermissions((Administrator) existing, existing.getAdmins(), adminIds);
        });
    }

    @Override
    public Set<String> addAdmin(String administratorId, String adminId) {
        // read access for all resources
        permissionService.addPermissions(Collections.singletonList(adminId), Collections.singletonList(Permissions.READ.getKey()), getAccessibleResourceIds(administratorId), administratorId);
        return super.addAdmin(administratorId, adminId);
    }

    @Override
    public Set<String> removeAdmin(String administratorId, String adminId) {
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
