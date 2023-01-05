package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.PolicyAccepted;
import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;

@Service
public class UserServiceImpl extends AbstractCrudItemService<User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final PrivacyPolicyService privacyPolicyService;

    @Autowired
    protected UserServiceImpl(ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              ParserService parserService,
                              PrivacyPolicyService privacyPolicyService) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.privacyPolicyService = privacyPolicyService;
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
    public User acceptPrivacyPolicy(String policyId, Authentication authentication) {
        User user = User.of(authentication);
        PrivacyPolicy policy = privacyPolicyService.get(policyId);

        FacetFilter filter = new FacetFilter();
        filter.addFilter("policyId", policy.getId());
        filter.addFilter("resource_internal_id", User.of(authentication).getId());
        Browsing<User> userBrowsing = getAll(filter);
        if (userBrowsing.getTotal() == 1) {
            //
            user = userBrowsing.getResults().get(0);

        } else if (userBrowsing.getTotal() > 1) {
            logger.error(String.format("More than one user with [id=%s] was found.", user.getId()));
            user = null;
        } else {
            //
            user = get(user.getId());
            if (user.getPoliciesAccepted() == null) {
                user.setPoliciesAccepted(new ArrayList<>());
            }
            user.getPoliciesAccepted().add(new PolicyAccepted(policy.getId(), new Date().getTime()));
            user = update(user.getId(), user);
        }

        return user;
    }

    @Override
    public void updateUserDetails(Authentication authentication) {
        User user = User.of(authentication);
        try {
            User existing = this.get(user.getId());
            if (!existing.getFullname().equals(user.getFullname())) {
                existing.setName(user.getName());
                existing.setSurname(user.getSurname());
            }
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
