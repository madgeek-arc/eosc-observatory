package eu.openaire.observatory.service;

import eu.openaire.observatory.widget.DashboardCode;
import eu.openaire.observatory.widget.Widget;
import eu.openaire.observatory.widget.DashboardDefaults;
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
public class DashboardDefaultsService extends AbstractCrudService<DashboardDefaults> {

    private static final String RESOURCE_TYPE = "dashboard_defaults";

    public DashboardDefaultsService(ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  VersionService versionService,
                                  ParserService parserService,
                                  ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
    }

    @Override
    public String createId(DashboardDefaults resource) {
        return "d-" + resource.getCode().wireValue() + "-" + resource.getType();
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public DashboardDefaults add(DashboardDefaults resource) {
        validate(resource);
        return super.add(resource);
    }

    @Override
    public DashboardDefaults update(String id, DashboardDefaults resource) throws ResourceNotFoundException {
        validate(resource);
        return super.update(id, resource);
    }

    /** Updates the defaults for the given code and type, creating them if none exist yet. */
    public DashboardDefaults upsertByCodeAndType(DashboardCode code, String type, DashboardDefaults resource) throws ResourceNotFoundException {
        Optional<DashboardDefaults> existing = getByCodeAndType(code, type);
        if (existing.isEmpty()) {
            resource.setId(null);
            return add(resource);
        }
        return update(existing.get().getId(), resource);
    }

    public DashboardDefaults deleteByCodeAndType(DashboardCode code, String type) throws ResourceNotFoundException {
        DashboardDefaults existing = getByCodeAndType(code, type)
                .orElseThrow(() -> new ResourceNotFoundException(type, "dashboard-defaults"));
        return delete(existing.getId());
    }

    public Optional<DashboardDefaults> getByCodeAndType(DashboardCode code, String type) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("code", code.wireValue());
        filter.addFilter("type", type);
        filter.setQuantity(1);
        Paging<DashboardDefaults> results = getResults(filter);
        return results.getResults().stream().findFirst();
    }

    public List<DashboardDefaults> getByCode(DashboardCode code) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("code", code.wireValue());
        filter.setQuantity(10000);
        Paging<DashboardDefaults> results = getResults(filter);
        return results.getResults();
    }

    private void validate(DashboardDefaults resource) {
        if (resource.getCode() == null) {
            throw new ValidationException("Dashboard code must not be null");
        }
        if (resource.getType() == null || resource.getType().isBlank()) {
            throw new ValidationException("Type must not be blank");
        }

        List<Widget> widgets = resource.getWidgets();
        if (widgets == null || widgets.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (Widget widget : widgets) {
            if (widget.getId() == null || widget.getId().isBlank()) {
                throw new ValidationException("Widget id must not be blank");
            }
            if (!seen.add(widget.getId())) {
                throw new ValidationException("Duplicate widget id: " + widget.getId());
            }
        }
    }
}
