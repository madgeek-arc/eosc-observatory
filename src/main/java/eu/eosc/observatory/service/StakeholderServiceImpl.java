package eu.eosc.observatory.service;

import eu.eosc.observatory.domain.Stakeholder;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.ResourceService;
import eu.openminted.registry.core.service.ResourceTypeService;
import eu.openminted.registry.core.service.SearchService;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StakeholderServiceImpl extends AbstractCrudItemService<Stakeholder> implements CrudItemService<Stakeholder> {

    private static final Logger logger = LogManager.getLogger(StakeholderServiceImpl.class);

    private static final String RESOURCE_TYPE = "stakeholder";

    @Autowired
    public StakeholderServiceImpl(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  ParserService parserService) {
        super(resourceTypeService, resourceService, searchService, parserService);
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public String createId(Stakeholder stakeholder) {
        String idSuffix;
        if (Stakeholder.StakeholderType.fromString(stakeholder.getType()) == Stakeholder.StakeholderType.COUNTRY) {
            idSuffix = stakeholder.getCountry();
        } else {
            idSuffix = stakeholder.getAssociationMember();
        }
        return String.format("sh-%s-%s", stakeholder.getType(), idSuffix);
    }
}
