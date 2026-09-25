package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.dto.DashboardOverrideSummary;
import eu.openaire.observatory.widget.Widget;
import eu.openaire.observatory.widget.DashboardDefaults;
import eu.openaire.observatory.widget.DashboardOverrides;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardOverrideService extends AbstractCrudService<DashboardOverrides> {

    private static final String RESOURCE_TYPE = "stakeholder_indicators";

    private final StakeholderService stakeholderService;
    private final DashboardDefaultsService defaultsService;

    public DashboardOverrideService(StakeholderService stakeholderService,
                                  DashboardDefaultsService defaultsService,
                                  ResourceTypeService resourceTypeService,
                                  ResourceService resourceService,
                                  SearchService searchService,
                                  VersionService versionService,
                                  ParserService parserService,
                                  ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.stakeholderService = stakeholderService;
        this.defaultsService = defaultsService;
    }

    @Override
    public String createId(DashboardOverrides resource) {
        Stakeholder sh = stakeholderService.get(resource.getStakeholderId());
        return "i-%s-%s-%s".formatted(resource.getCode(), sh.getType(), sh.getCountry());
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public DashboardOverrides add(DashboardOverrides resource) {
        validate(resource);
        return super.add(resource);
    }

    @Override
    public DashboardOverrides update(String id, DashboardOverrides resource) throws ResourceNotFoundException {
        validate(resource);
        return super.update(id, resource);
    }

    public DashboardOverrides deleteByCodeAndStakeholderId(String code, String stakeholderId) throws ResourceNotFoundException {
        DashboardOverrides existing = getByCodeAndStakeholderId(code, stakeholderId)
                .orElseThrow(() -> new ResourceNotFoundException("stakeholderId", stakeholderId));
        return delete(existing.getId());
    }

    public DashboardOverrides upsert(DashboardOverrides resource) throws ResourceNotFoundException {
        Optional<DashboardOverrides> existing = getByCodeAndStakeholderId(resource.getCode(), resource.getStakeholderId());
        if (existing.isPresent()) {
            return update(existing.get().getId(), resource);
        }
        return add(resource);
    }

    public Optional<DashboardOverrides> getByCodeAndStakeholderId(String code, String stakeholderId) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("code", code);
        filter.addFilter("stakeholderId", stakeholderId);
        filter.setQuantity(1);
        Paging<DashboardOverrides> results = getResults(filter);
        return results.getResults().stream().findFirst();
    }

    public List<Widget> getEffectiveWidgets(String code, String stakeholderId) {
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        List<Widget> defaults = defaultsService.getByCodeAndType(code, stakeholder.getType())
                .map(DashboardDefaults::getWidgets)
                .orElse(List.of());

        Optional<DashboardOverrides> override = getByCodeAndStakeholderId(code, stakeholderId);
        if (override.isEmpty() || override.get().getWidgets() == null) {
            return defaults;
        }

        Map<String, Boolean> visibilityOverrides = override.get().getWidgets().stream()
                .collect(Collectors.toMap(Widget::getId, Widget::isVisible));

        return defaults.stream()
                .map(widget -> {
                    if (!visibilityOverrides.containsKey(widget.getId())) {
                        return widget;
                    }
                    Widget effective = new Widget();
                    effective.setId(widget.getId());
                    effective.setLabel(widget.getLabel());
                    effective.setFormat(widget.getFormat());
                    effective.setGroup(widget.getGroup());
                    effective.setVisible(visibilityOverrides.get(widget.getId()));
                    return effective;
                })
                .collect(Collectors.toList());
    }

    public List<DashboardOverrideSummary> getStakeholdersWithOverrideStatus(String code, String type) {
        FacetFilter overrideFilter = new FacetFilter();
        overrideFilter.setResourceType(RESOURCE_TYPE);
        overrideFilter.addFilter("code", code);
        overrideFilter.setQuantity(10000);
        Paging<DashboardOverrides> allOverrides = getResults(overrideFilter);
        Set<String> stakeholderIdsWithOverrides = allOverrides.getResults().stream()
                .map(DashboardOverrides::getStakeholderId)
                .collect(Collectors.toSet());

        FacetFilter stakeholderFilter = new FacetFilter();
        stakeholderFilter.setQuantity(10000);
        stakeholderFilter.addFilter("type", type);
        return stakeholderService.getAll(stakeholderFilter).getResults().stream()
                .map(sh -> new DashboardOverrideSummary(sh.getId(), sh.getCountry(), stakeholderIdsWithOverrides.contains(sh.getId())))
                .collect(Collectors.toList());
    }

    private void validate(DashboardOverrides resource) {
        if (resource.getCode() == null || resource.getCode().isBlank()) {
            throw new ValidationException("Widget catalog code must not be blank");
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

        Stakeholder stakeholder = stakeholderService.get(resource.getStakeholderId());
        Set<String> validIds = defaultsService.getByCodeAndType(resource.getCode(), stakeholder.getType())
                .map(DashboardDefaults::getWidgets)
                .map(list -> list.stream().map(Widget::getId).collect(Collectors.toSet()))
                .orElse(Set.of());

        if (!validIds.isEmpty()) {
            for (String id : seen) {
                if (!validIds.contains(id)) {
                    throw new ValidationException("Unknown widget id: " + id);
                }
            }
        }
    }
}
