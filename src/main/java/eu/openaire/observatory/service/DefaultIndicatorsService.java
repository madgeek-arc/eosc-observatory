package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DefaultIndicators;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.catalogue.service.id.IdGenerator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DefaultIndicatorsService extends AbstractCrudService<DefaultIndicators> {

    private static final String RESOURCE_TYPE = "default_indicators";

    public DefaultIndicatorsService(ResourceTypeService resourceTypeService,
                                    ResourceService resourceService,
                                    SearchService searchService,
                                    VersionService versionService,
                                    ParserService parserService,
                                    ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
    }

    @Override
    public String createId(DefaultIndicators resource) {
        return "i-" + resource.getType();
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    public Optional<DefaultIndicators> getByType(String type) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("type", type);
        filter.setQuantity(1);
        Paging<DefaultIndicators> results = getResults(filter);
        return results.getResults().stream().findFirst();
    }
}
