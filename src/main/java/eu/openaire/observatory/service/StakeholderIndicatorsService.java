package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DefaultIndicators;
import eu.openaire.observatory.domain.Indicator;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.StakeholderIndicatorsOverride;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
}
