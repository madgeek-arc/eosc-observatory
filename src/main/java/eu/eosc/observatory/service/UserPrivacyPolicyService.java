package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserPrivacyPolicyService extends AbstractCrudItemService<PrivacyPolicy> implements PrivacyPolicyService {

    private static final Logger logger = LogManager.getLogger(UserPrivacyPolicyService.class);

    private final UserService userService;

    @Autowired
    public UserPrivacyPolicyService(ResourceTypeService resourceTypeService,
                                    ResourceService resourceService,
                                    SearchService searchService,
                                    ParserService parserService,
                                    UserService userService) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.userService = userService;
    }

    @Override
    public String createId(PrivacyPolicy resource) {
        return resource.getId();
    }

    @Override
    public String getResourceType() {
        return "privacy_policy";
    }

    @Override
    public PrivacyPolicy getLatestByType(String type) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("type", type);
        filter.addOrderBy("activationDate", "desc");
        Browsing<PrivacyPolicy> policies = getAll(filter);
        return policies.getTotal() > 0 ? policies.getResults().get(0) : null;
    }

    @Override
    public boolean hasAcceptedPolicy(String policyId, String userId) {
        FacetFilter filter = new FacetFilter();
        filter.addFilter("user_id", userId);
        filter.addFilter("policyId", policyId);
        Browsing<User> users = userService.getAll(filter);
        return users.getTotal() >= 1;
    }
}
