package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DashboardItem;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.domain.UserDashboard;
import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.IndicatorPreset;
import eu.openaire.observatory.indicator.IndicatorPresetCatalog;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.PresetScopeValidation;
import eu.openaire.observatory.utils.UserIds;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One dashboard per user, keyed by the owner's normalized email. Private to its owner. */
@Service
public class UserDashboardService extends AbstractCrudService<UserDashboard> {

    private static final String RESOURCE_TYPE = "user_dashboard";

    private final IndicatorQueryValidator indicatorQueryValidator;
    private final ActorContext actorContext;
    private final IndicatorPresetCatalog presetCatalog;
    private final IndicatorDefinitionCatalog definitionCatalog;

    public UserDashboardService(IndicatorQueryValidator indicatorQueryValidator,
                                ActorContext actorContext,
                                IndicatorPresetCatalog presetCatalog,
                                IndicatorDefinitionCatalog definitionCatalog,
                                ResourceTypeService resourceTypeService,
                                ResourceService resourceService,
                                SearchService searchService,
                                VersionService versionService,
                                ParserService parserService,
                                ModelResponseValidator validator) {
        super(resourceTypeService, resourceService, searchService, versionService, parserService, validator);
        this.indicatorQueryValidator = indicatorQueryValidator;
        this.actorContext = actorContext;
        this.presetCatalog = presetCatalog;
        this.definitionCatalog = definitionCatalog;
    }

    @Override
    public String createId(UserDashboard resource) {
        return resource.getId();
    }

    @Override
    public String getResourceType() {
        return RESOURCE_TYPE;
    }

    /** Never persists on read — a user with no saved dashboard yet just sees an empty one. */
    public UserDashboard getOrCreateForCurrentUser(Authentication authentication) {
        return getOrCreate(User.getId(authentication));
    }

    /** Admin/support variant of {@link #getOrCreateForCurrentUser}: reads any user's dashboard by id. */
    public UserDashboard getOrCreate(String ownerId) {
        String id = UserIds.normalize(ownerId);
        try {
            return get(id);
        } catch (ResourceNotFoundException e) {
            UserDashboard dashboard = new UserDashboard();
            dashboard.setId(id);
            dashboard.setItems(List.of());
            return dashboard;
        }
    }

    /** Replaces the current user's full dashboard (items plus shared filters) in one call. */
    public UserDashboard save(Authentication authentication, UserDashboard request) {
        return save(User.getId(authentication), authentication, request);
    }

    /**
     * Admin/support variant of {@link #save(Authentication, UserDashboard)} for {@code ownerId}.
     * {@code actor} is recorded as the one who performed the write. {@code id}/{@code metadata} on
     * {@code request} are ignored.
     */
    public UserDashboard save(String ownerId, Authentication actor, UserDashboard request) {
        String id = UserIds.normalize(ownerId);

        UserDashboard dashboard = normalizeAndValidate(request);
        dashboard.setId(id);

        try {
            UserDashboard existing = get(id);
            Metadata metadata = existing.getMetadata();
            metadata.setModifiedBy(User.getId(actor));
            metadata.setModificationDate(new Date());
            dashboard.setMetadata(metadata);
            return update(id, dashboard);
        } catch (ResourceNotFoundException e) {
            dashboard.setMetadata(new Metadata(actor));
            return add(dashboard);
        }
    }

    private UserDashboard normalizeAndValidate(UserDashboard request) {
        List<DashboardItem> items = request.getItems() == null ? List.of() : request.getItems();
        List<String> countries = request.getCountries();
        Integer yearFrom = request.getYearFrom();
        Integer yearTo = request.getYearTo();
        List<AggregationType> seriesAggregations = request.getSeriesAggregations() == null
                ? List.of() : request.getSeriesAggregations();

        List<DashboardItem> resolvedItems = new ArrayList<>();
        List<IndicatorDefinition> resolvedIndicators = new ArrayList<>();
        for (DashboardItem item : items) {
            if (item.getIndicatorId() == null || item.getIndicatorId().isBlank()) {
                throw new ValidationException("Dashboard item indicatorId must not be null");
            }
            IndicatorPreset preset = presetCatalog.get(item.getIndicatorId());
            if (preset == null) {
                throw new ValidationException("Unknown indicator id: " + item.getIndicatorId());
            }
            resolvedItems.add(item);
            resolvedIndicators.add(definitionCatalog.get(preset.indicatorCodeFor(countries, yearFrom, yearTo)));
        }

        Set<String> seenIds = new HashSet<>();
        List<DashboardItem> normalized = new ArrayList<>();
        for (int i = 0; i < resolvedItems.size(); i++) {
            DashboardItem item = resolvedItems.get(i);
            IndicatorPreset preset = presetCatalog.get(item.getIndicatorId());
            IndicatorDefinition indicator = resolvedIndicators.get(i);

            PresetScopeValidation.validate(
                preset, indicator.timePolicy().supported(), yearFrom, yearTo, seriesAggregations);

            IndicatorQuery query = preset.toIndicatorQuery(indicator, countries, yearFrom, yearTo, seriesAggregations);
            indicatorQueryValidator.compile(query, actorContext);

            String itemId = (item.getId() == null || item.getId().isBlank())
                    ? UUID.randomUUID().toString()
                    : item.getId();
            if (!seenIds.add(itemId)) {
                throw new ValidationException("Duplicate dashboard item id: " + itemId);
            }
            item.setId(itemId);
            normalized.add(item);
        }

        UserDashboard dashboard = new UserDashboard();
        dashboard.setItems(normalized);
        dashboard.setCountries(countries);
        dashboard.setYearFrom(yearFrom);
        dashboard.setYearTo(yearTo);
        dashboard.setSeriesAggregations(request.getSeriesAggregations());
        return dashboard;
    }
}
