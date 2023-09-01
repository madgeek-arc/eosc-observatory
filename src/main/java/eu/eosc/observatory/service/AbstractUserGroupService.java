package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.eosc.observatory.domain.UserGroup;
import eu.eosc.observatory.dto.GroupMembers;
import eu.openminted.registry.core.service.*;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractUserGroupService<T extends UserGroup> extends AbstractCrudService<T> implements UserGroupService {

    protected final UserService userService;
    protected AbstractUserGroupService(UserService userService,
                                       ResourceTypeService resourceTypeService,
                                       ResourceService resourceService,
                                       SearchService searchService,
                                       VersionService versionService,
                                       ParserService parserService) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService);
        this.userService = userService;
    }

    protected Set<User> getUsers(Set<String> userIds) {
        return userIds.stream().map(userService::get).collect(Collectors.toSet());
    }

    @Override
    public Set<User> getMembers(String groupId) {
        return getUsers(((UserGroup) super.get(getResourceType(), groupId)).getMembers());
    }

    @Override
    public Set<User> getAdmins(String groupId) {
        return getUsers(((UserGroup) super.get(getResourceType(), groupId)).getAdmins());
    }
}
