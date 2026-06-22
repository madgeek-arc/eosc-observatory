package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DefaultIndicators;
import eu.openaire.observatory.domain.Indicator;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.StakeholderIndicatorsOverride;
import eu.openaire.observatory.dto.StakeholderIndicatorsSummary;
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
public class StakeholderIndicatorsService extends AbstractCrudService<StakeholderIndicatorsOverride> {

    private static final String RESOURCE_TYPE = "stakeholder_indicators";

    private final StakeholderService stakeholderService;
    private final DefaultIndicatorsService defaultIndicatorsService;

    public StakeholderIndicatorsService(StakeholderService stakeholderService,
                                        DefaultIndicatorsService defaultIndicatorsService,
                                        ResourceTypeService resourceTypeService,
                                        ResourceService resourceService,
                                        SearchService searchService,
                                        VersionService versionService,
                                        ParserService parserService,
                                        ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.stakeholderService = stakeholderService;
        this.defaultIndicatorsService = defaultIndicatorsService;
    }

    @Override
    public String createId(StakeholderIndicatorsOverride resource) {
        Stakeholder sh = stakeholderService.get(resource.getStakeholderId());
        return "i-%s-%s".formatted(sh.getType(), sh.getCountry());
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    @Override
    public StakeholderIndicatorsOverride add(StakeholderIndicatorsOverride resource) {
        validate(resource);
        return super.add(resource);
    }

    @Override
    public StakeholderIndicatorsOverride update(String id, StakeholderIndicatorsOverride resource) throws ResourceNotFoundException {
        validate(resource);
        return super.update(id, resource);
    }

    public StakeholderIndicatorsOverride deleteByStakeholderId(String stakeholderId) throws ResourceNotFoundException {
        StakeholderIndicatorsOverride existing = getByStakeholderId(stakeholderId)
                .orElseThrow(() -> new ResourceNotFoundException("stakeholderId", stakeholderId));
        return delete(existing.getId());
    }

    public StakeholderIndicatorsOverride upsert(StakeholderIndicatorsOverride resource) throws ResourceNotFoundException {
        Optional<StakeholderIndicatorsOverride> existing = getByStakeholderId(resource.getStakeholderId());
        if (existing.isPresent()) {
            return update(existing.get().getId(), resource);
        }
        return add(resource);
    }

    public Optional<StakeholderIndicatorsOverride> getByStakeholderId(String stakeholderId) {
        FacetFilter filter = new FacetFilter();
        filter.setResourceType(RESOURCE_TYPE);
        filter.addFilter("stakeholderId", stakeholderId);
        filter.setQuantity(1);
        Paging<StakeholderIndicatorsOverride> results = getResults(filter);
        return results.getResults().stream().findFirst();
    }

    public List<Indicator> getEffectiveIndicators(String stakeholderId) {
        Stakeholder stakeholder = stakeholderService.get(stakeholderId);
        List<Indicator> defaults = defaultIndicatorsService.getByType(stakeholder.getType())
                .map(DefaultIndicators::getIndicators)
                .orElse(List.of());

        Optional<StakeholderIndicatorsOverride> override = getByStakeholderId(stakeholderId);
        if (override.isEmpty() || override.get().getIndicators() == null) {
            return defaults;
        }

        Map<String, Boolean> visibilityOverrides = override.get().getIndicators().stream()
                .collect(Collectors.toMap(Indicator::getId, Indicator::isVisible));

        return defaults.stream()
                .map(indicator -> {
                    if (!visibilityOverrides.containsKey(indicator.getId())) {
                        return indicator;
                    }
                    Indicator effective = new Indicator();
                    effective.setId(indicator.getId());
                    effective.setLabel(indicator.getLabel());
                    effective.setFormat(indicator.getFormat());
                    effective.setGroup(indicator.getGroup());
                    effective.setVisible(visibilityOverrides.get(indicator.getId()));
                    return effective;
                })
                .collect(Collectors.toList());
    }

    public List<StakeholderIndicatorsSummary> getStakeholdersWithOverrideStatus(String type) {
        FacetFilter overrideFilter = new FacetFilter();
        overrideFilter.setResourceType(RESOURCE_TYPE);
        overrideFilter.setQuantity(10000);
        Paging<StakeholderIndicatorsOverride> allOverrides = getResults(overrideFilter);
        Set<String> stakeholderIdsWithOverrides = allOverrides.getResults().stream()
                .map(StakeholderIndicatorsOverride::getStakeholderId)
                .collect(Collectors.toSet());

        FacetFilter stakeholderFilter = new FacetFilter();
        stakeholderFilter.setQuantity(10000);
        stakeholderFilter.addFilter("type", type);
        return stakeholderService.getAll(stakeholderFilter).getResults().stream()
                .map(sh -> new StakeholderIndicatorsSummary(sh.getId(), sh.getCountry(), stakeholderIdsWithOverrides.contains(sh.getId())))
                .collect(Collectors.toList());
    }

    private void validate(StakeholderIndicatorsOverride resource) {
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

        Stakeholder stakeholder = stakeholderService.get(resource.getStakeholderId());
        Set<String> validIds = defaultIndicatorsService.getByType(stakeholder.getType())
                .map(DefaultIndicators::getIndicators)
                .map(list -> list.stream().map(Indicator::getId).collect(Collectors.toSet()))
                .orElse(Set.of());

        if (!validIds.isEmpty()) {
            for (String id : seen) {
                if (!validIds.contains(id)) {
                    throw new ValidationException("Unknown indicator id: " + id);
                }
            }
        }
    }
}
