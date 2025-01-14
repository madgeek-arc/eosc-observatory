package eu.eosc.observatory.service;

import eu.eosc.observatory.configuration.ApplicationProperties;
import eu.eosc.observatory.domain.*;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.service.*;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

@Service
public class UserServiceImpl extends AbstractCrudService<User> implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final PrivacyPolicyService privacyPolicyService;
    private final CrudService<Stakeholder> stakeholderCrudService;
    private final CrudService<Coordinator> coordinatorCrudService;
    private final CrudService<Administrator> administratorCrudService;
    private final ApplicationProperties applicationProperties;

    protected UserServiceImpl(ResourceTypeService resourceTypeService,
                              ResourceService resourceService,
                              SearchService searchService,
                              VersionService versionService,
                              ParserService parserService,
                              PrivacyPolicyService privacyPolicyService,
                              @Lazy CrudService<Stakeholder> stakeholderCrudService,
                              @Lazy CrudService<Coordinator> coordinatorCrudService,
                              @Lazy CrudService<Administrator> administratorCrudService,
                              ApplicationProperties applicationProperties) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService);
        this.privacyPolicyService = privacyPolicyService;
        this.stakeholderCrudService = stakeholderCrudService;
        this.coordinatorCrudService = coordinatorCrudService;
        this.administratorCrudService = administratorCrudService;
        this.applicationProperties = applicationProperties;
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
    public UserInfo getUserInfo(String userId) {
        User user = getUser(userId);
        return createUserInfo(user);
    }

    @Override
    public UserInfo getUserInfo(Authentication authentication) {
        User user;
        try {
            user = get(User.getId(authentication));
        } catch (ResourceNotFoundException e) {
            logger.debug("User not found in DB");
            user = User.of(authentication);
        }
        return createUserInfo(user);
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
                existing.setFullname(user.getFullname());
                this.update(existing.getId(), existing); // save previous user object with updated name/surname
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

    private UserInfo createUserInfo(User user) {
        UserInfo info = new UserInfo();
        info.setUser(user);
        info.setAdmin(applicationProperties.getAdmins().contains(user.getEmail()));
        info.setStakeholders(new HashSet<>());
        info.setCoordinators(new HashSet<>());
        info.setAdministrators(new HashSet<>());

        info.getStakeholders().addAll(stakeholderCrudService.getWithFilter("users", user.getId()));
        info.getCoordinators().addAll(coordinatorCrudService.getWithFilter("users", user.getId()));
        info.getAdministrators().addAll(administratorCrudService.getWithFilter("users", user.getId()));
        return info;
    }
}
