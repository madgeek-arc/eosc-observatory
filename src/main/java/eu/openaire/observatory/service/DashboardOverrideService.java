package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.dto.DashboardOverrideSummary;
import eu.openaire.observatory.widget.DashboardCode;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DashboardOverrideService extends AbstractCrudService<DashboardOverrides> {

    private static final String RESOURCE_TYPE = "dashboard_overrides";

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
        return "d-%s-%s-%s".formatted(resource.getCode().wireValue(), resource.getType(), resource.getGroupId());
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

    public DashboardOverrides deleteByCodeAndTypeAndGroupId(DashboardCode code, String type, String groupId) throws ResourceNotFoundException {
        DashboardOverrides existing = getByCodeAndTypeAndGroupId(code, type, groupId)
                .orElseThrow(() -> new ResourceNotFoundException(groupId, "dashboard-overrides"));
        return delete(existing.getId());
    }

    public DashboardOverrides upsert(DashboardOverrides resource) throws ResourceNotFoundException {
        validate(resource);
        requireGroupOfType(resource.getType(), resource.getGroupId());
        Optional<DashboardOverrides> existing = getByCodeAndTypeAndGroupId(
                resource.getCode(), resource.getType(), resource.getGroupId());
        if (existing.isPresent()) {
            return update(existing.get().getId(), resource);
        }
        return add(resource);
    }

    /** Fails with {@link ResourceNotFoundException} unless the group exists and is of the given type. */
    public void requireGroupOfType(String type, String groupId) throws ResourceNotFoundException {
        Stakeholder group = stakeholderService.get(groupId);
        if (group == null || !Objects.equals(group.getType(), type)) {
            throw new ResourceNotFoundException(groupId, "dashboard-overrides");
        }
    }

    public Optional<DashboardOverrides> getByCodeAndTypeAndGroupId(DashboardCode code, String type, String groupId) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("code", code.wireValue());
        filter.addFilter("type", type);
        filter.addFilter("groupId", groupId);
        filter.setQuantity(1);
        Paging<DashboardOverrides> results = getResults(filter);
        return results.getResults().stream().findFirst();
    }

    public List<Widget> getEffectiveWidgets(DashboardCode code, String type, String groupId) {
        List<Widget> defaults = defaultsService.getByCodeAndType(code, type)
                .map(DashboardDefaults::getWidgets)
                .orElse(List.of());

        Optional<DashboardOverrides> override = getByCodeAndTypeAndGroupId(code, type, groupId);
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
                    Widget effective = new Widget(widget);
                    effective.setVisible(visibilityOverrides.get(widget.getId()));
                    return effective;
                })
                .collect(Collectors.toList());
    }

    public List<DashboardOverrideSummary> getGroupsWithOverrideStatus(DashboardCode code, String type) {
        FacetFilter overrideFilter = new FacetFilter();
        overrideFilter.setResourceType(RESOURCE_TYPE);
        overrideFilter.addFilter("code", code.wireValue());
        overrideFilter.setQuantity(10000);
        Paging<DashboardOverrides> allOverrides = getResults(overrideFilter);
        Set<String> groupIdsWithOverrides = allOverrides.getResults().stream()
                .map(DashboardOverrides::getGroupId)
                .collect(Collectors.toSet());

        FacetFilter stakeholderFilter = new FacetFilter();
        stakeholderFilter.setQuantity(10000);
        stakeholderFilter.addFilter("type", type);
        return stakeholderService.getAll(stakeholderFilter).getResults().stream()
                .map(sh -> new DashboardOverrideSummary(sh.getId(), sh.getName(), groupIdsWithOverrides.contains(sh.getId())))
                .collect(Collectors.toList());
    }

    private void validate(DashboardOverrides resource) {
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

        Set<String> validIds = defaultsService.getByCodeAndType(resource.getCode(), resource.getType())
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
