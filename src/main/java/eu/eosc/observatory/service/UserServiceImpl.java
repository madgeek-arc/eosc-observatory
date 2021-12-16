package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractCrudItemService<User> implements UserService {

    private static final Logger logger = LogManager.getLogger(UserServiceImpl.class);

    @Autowired
    protected UserServiceImpl(ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              ParserService parserService) {
        super(resourceTypeService, resourceService, searchService, parserService);
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
    public User getUser(String id) {
        User user = null;
        try {
            user = get(id);
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = new User();
            user.setEmail(id);
        }
        return user;
    }

    @Override
    public void updateUserConsent(String id, boolean consent) {
        logger.info(String.format("Updating user consent: [userId=%s] [consent=%s]", id, consent));
        User user = get(id);
        user.setConsent(consent);
        update(user.getId(), user);
    }

    @Override
    public void updateUserInfo(Authentication authentication) {
        User user = User.of(authentication);
        try {
            this.get(user.getId());
        } catch (ResourceNotFoundException e) {
            logger.debug(String.format("User not found! Adding User to database [user=%s]", user));
            this.add(user);
        }
    }

    @Override
    public void purge(String id) throws ResourceNotFoundException {
        throw new UnsupportedOperationException("Not implemented yet");
//        User user = delete(id);
        // delete user from everywhere
        // stakeholders
        // surveyAnswers
        // survey metadata
        // permissions
        // core versions of the above
    }
}
