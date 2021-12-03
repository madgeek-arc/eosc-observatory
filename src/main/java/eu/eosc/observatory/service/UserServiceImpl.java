package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractCrudItemService<User> implements UserService {

    private final StakeholderService stakeholderService;
    private final PermissionsService permissionsService;

    @Autowired
    protected UserServiceImpl(ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              ParserService parserService,
                              StakeholderService stakeholderService,
                              PermissionsService permissionsService) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.stakeholderService = stakeholderService;
        this.permissionsService = permissionsService;
    }

    @Override
    public String createId(User resource) {
        return resource.getEmail();
    }

    @Override
    public String getResourceType() {
        return "user";
    }

    @Override
    public void updateUserConsent(String id, boolean consent) {
        User user = get(id);
        user.setConsent(consent);
        update(user.getId(), user);
    }

    @Override
    public void updateUserInfo(Authentication authentication) {
        User user = User.of(authentication);
        try {
            this.update(user.getId(), user);
        } catch (ResourceNotFoundException e) {
            this.add(user);
        }
    }

    @Override
    public void purge(String id) throws ResourceNotFoundException {
        User user = delete(id);
        // delete user from everywhere
        // stakeholders
        // surveyAnswers
        // survey metadata
        // permissions
        // core versions of the above
    }
}
