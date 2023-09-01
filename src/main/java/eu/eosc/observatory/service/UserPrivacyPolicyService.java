package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.PrivacyPolicy;
import eu.eosc.observatory.domain.User;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


@Service
public class UserPrivacyPolicyService extends AbstractCrudService<PrivacyPolicy> implements PrivacyPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(UserPrivacyPolicyService.class);

    private final UserService userService;

    @Autowired
    public UserPrivacyPolicyService(ResourceTypeService resourceTypeService,
                                    ResourceService resourceService,
                                    SearchService searchService,
                                    VersionService versionService,
                                    ParserService parserService,
                                    @Lazy UserService userService) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService);
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
        filter.addFilter("resource_internal_id", userId);
        filter.addFilter("policyId", policyId);
        Browsing<User> users = userService.getAll(filter);
        return users.getTotal() >= 1;
    }
}
