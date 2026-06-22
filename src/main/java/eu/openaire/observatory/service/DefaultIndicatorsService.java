package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DefaultIndicators;
import eu.openaire.observatory.domain.Indicator;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Override
    public DefaultIndicators add(DefaultIndicators resource) {
        validate(resource);
        return super.add(resource);
    }

    @Override
    public DefaultIndicators update(String id, DefaultIndicators resource) throws ResourceNotFoundException {
        validate(resource);
        return super.update(id, resource);
    }

    public Optional<DefaultIndicators> getByType(String type) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("type", type);
        filter.setQuantity(1);
        Paging<DefaultIndicators> results = getResults(filter);
        return results.getResults().stream().findFirst();
    }

    private void validate(DefaultIndicators resource) {
        List<Indicator> indicators = resource.getIndicators();
        if (indicators == null || indicators.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Indicator indicator : indicators) {
            if (indicator.getId() == null || indicator.getId().isBlank()) {
                throw new ValidationException("Indicator id must not be blank");
            }
            if (!seen.add(indicator.getId())) {
                throw new ValidationException("Duplicate indicator id: " + indicator.getId());
            }
        }
    }
}
