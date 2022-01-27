package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.UserAcceptedPolicy;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import gr.athenarc.catalogue.service.id.IdGenerator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAcceptedPolicyCrudService extends AbstractCrudItemService<UserAcceptedPolicy> {

    private static final Logger logger = LogManager.getLogger(UserAcceptedPolicyCrudService.class);

    private final IdGenerator<String> idGenerator;

    @Autowired
    public UserAcceptedPolicyCrudService(ResourceTypeService resourceTypeService,
                                         ResourceService resourceService,
                                         SearchService searchService,
                                         ParserService parserService,
                                         IdGenerator<String> idGenerator) {
        super(resourceTypeService, resourceService, searchService, parserService);
        this.idGenerator = idGenerator;
    }

    @Override
    public String createId(UserAcceptedPolicy resource) {
        return idGenerator.createId("uap-", 8);
    }

    @Override
    public String getResourceType() {
        return "user_accepted_policy";
    }
}
