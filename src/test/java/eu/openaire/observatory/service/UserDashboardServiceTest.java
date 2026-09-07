package eu.openaire.observatory.service;

import eu.openaire.observatory.domain.DashboardItem;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.UserDashboard;
import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorExecutionBinding;
import eu.openaire.observatory.indicator.model.IndicatorFormat;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.model.IndicatorStatus;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.CountryScope;
import eu.openaire.observatory.indicator.model.NullHandling;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.RenderStyle;
import eu.openaire.observatory.indicator.model.TimeScope;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.ViewSelector;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.IndicatorPreset;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.IndicatorPresetCatalog;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.InvalidPresetScopeException;
import eu.openaire.observatory.indicator.validation.SeriesAggregationNotAllowedException;
import eu.openaire.observatory.utils.OidcTestUtils;
import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import gr.uoa.di.madgik.catalogue.service.ModelResponseValidator;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.registry.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDashboardServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String TREND_WIDGET_ID = "widget-trend";
    private static final String NON_TREND_WIDGET_ID = "widget-non-trend";

    @Mock IndicatorQueryValidator indicatorQueryValidator;
    @Mock ActorContext actorContext;
    @Mock IndicatorPresetCatalog presetCatalog;
    @Mock IndicatorDefinitionCatalog definitionCatalog;
    @Mock ResourceTypeService resourceTypeService;
    @Mock ResourceService resourceService;
    @Mock SearchService searchService;
    @Mock VersionService versionService;
    @Mock ParserService parserService;
    @Mock ModelResponseValidator validator;

    private UserDashboardService service;
    private Authentication authentication;

    private IndicatorPreset trendDefinition;
    private IndicatorDefinition trendIndicator;
    private IndicatorPreset nonTrendDefinition;
    private IndicatorDefinition nonTrendIndicator;

    @BeforeEach
    void setUp() {
        service = spy(new UserDashboardService(
                indicatorQueryValidator, actorContext, presetCatalog, definitionCatalog,
                resourceTypeService, resourceService, searchService,
                versionService, parserService, validator));
        authentication = OidcTestUtils.oidcAuthentication(EMAIL);

        trendDefinition = new IndicatorPreset(
                TREND_WIDGET_ID, "Trend Widget", IndicatorGroup.OPEN_ACCESS_PUBLICATIONS, List.of(), false, null,
                List.of(AggregationType.AVG, AggregationType.MEDIAN), "indicator.trend", Map.of(), 10);
        trendIndicator = indicatorDefinition("indicator.trend", true,
                Set.of(new IndicatorDimensionBinding(
                        IndicatorCodes.COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ, FilterOperator.IN), false)));

        nonTrendDefinition = new IndicatorPreset(
                NON_TREND_WIDGET_ID, "Non-Trend Widget", IndicatorGroup.OPEN_ACCESS_PUBLICATIONS, List.of(), false, null,
                List.of(), "indicator.non-trend", Map.of(), 20);
        nonTrendIndicator = indicatorDefinition("indicator.non-trend", false, Set.of());

        lenient().when(presetCatalog.get(TREND_WIDGET_ID)).thenReturn(trendDefinition);
        lenient().when(definitionCatalog.get("indicator.trend")).thenReturn(trendIndicator);
        lenient().when(presetCatalog.get(NON_TREND_WIDGET_ID)).thenReturn(nonTrendDefinition);
        lenient().when(definitionCatalog.get("indicator.non-trend")).thenReturn(nonTrendIndicator);
    }

    // --- createId ---

    @Test
    void createId_returnsResourceOwnId() {
        UserDashboard dashboard = new UserDashboard();
        dashboard.setId(EMAIL);

        assertEquals(EMAIL, service.createId(dashboard));
    }

    // --- getOrCreateForCurrentUser ---

    @Test
    void getOrCreateForCurrentUser_returnsExistingWhenPresent() {
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        existing.setItems(List.of(dashboardItem("i-1", TREND_WIDGET_ID)));
        doReturn(existing).when(service).get(EMAIL);

        UserDashboard result = service.getOrCreateForCurrentUser(authentication);

        assertSame(existing, result);
    }

    @Test
    void getOrCreateForCurrentUser_returnsEmptyTransientDashboardWhenMissing() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);

        UserDashboard result = service.getOrCreateForCurrentUser(authentication);

        assertEquals(EMAIL, result.getId());
        assertTrue(result.getItems().isEmpty());
        verify(resourceService, never()).addResource(any());
    }

    // --- getOrCreate (admin/support, by explicit id) ---

    @Test
    void getOrCreate_returnsExistingWhenPresent() {
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        doReturn(existing).when(service).get(EMAIL);

        assertSame(existing, service.getOrCreate(EMAIL));
    }

    @Test
    void getOrCreate_normalizesOwnerIdCasing() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);

        UserDashboard result = service.getOrCreate("User@Example.com");

        assertEquals(EMAIL, result.getId());
        verify(service).get(EMAIL);
    }

    // --- save: new dashboard ---

    @Test
    void save_createsNewDashboardWhenNoneExists() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        DashboardItem item = dashboardItem("i-1", TREND_WIDGET_ID);

        service.save(authentication, trendRequest(item));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        UserDashboard saved = captor.getValue();
        assertEquals(EMAIL, saved.getId());
        assertEquals(EMAIL, saved.getMetadata().getCreatedBy());
        assertEquals(1, saved.getItems().size());
    }

    // --- save: existing dashboard ---

    @Test
    void save_updatesExistingDashboardPreservingCreationMetadata() throws Exception {
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        Metadata originalMetadata = new Metadata();
        originalMetadata.setCreatedBy(EMAIL);
        originalMetadata.setCreationDate(new Date(0));
        existing.setMetadata(originalMetadata);
        doReturn(existing).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).update(eq(EMAIL), any());

        service.save(authentication, trendRequest(dashboardItem("i-1", TREND_WIDGET_ID)));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).update(eq(EMAIL), captor.capture());
        UserDashboard saved = captor.getValue();
        assertEquals(new Date(0), saved.getMetadata().getCreationDate(), "creation metadata must be preserved");
        assertEquals(EMAIL, saved.getMetadata().getModifiedBy());
    }

    // --- save: admin/support on behalf of another user ---

    @Test
    void save_withExplicitOwnerId_targetsThatOwnerNotTheActor() {
        String adminEmail = "admin@example.com";
        Authentication adminAuth = OidcTestUtils.oidcAuthentication(adminEmail);
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());

        service.save(EMAIL, adminAuth, trendRequest(dashboardItem("i-1", TREND_WIDGET_ID)));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        UserDashboard saved = captor.getValue();
        assertEquals(EMAIL, saved.getId(), "dashboard is still owned by the target user");
        assertEquals(adminEmail, saved.getMetadata().getCreatedBy(), "audit trail records who actually wrote it");
    }

    @Test
    void save_withExplicitOwnerId_recordsActorAsModifiedByOnUpdate() throws Exception {
        String adminEmail = "admin@example.com";
        Authentication adminAuth = OidcTestUtils.oidcAuthentication(adminEmail);
        UserDashboard existing = new UserDashboard();
        existing.setId(EMAIL);
        Metadata originalMetadata = new Metadata();
        originalMetadata.setCreatedBy(EMAIL);
        originalMetadata.setCreationDate(new Date(0));
        existing.setMetadata(originalMetadata);
        doReturn(existing).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).update(eq(EMAIL), any());

        service.save(EMAIL, adminAuth, trendRequest(dashboardItem("i-1", TREND_WIDGET_ID)));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).update(eq(EMAIL), captor.capture());
        assertEquals(adminEmail, captor.getValue().getMetadata().getModifiedBy());
    }

    // --- validation: indicatorId ---

    @Test
    void save_rejectsNullIndicatorId() {
        DashboardItem item = dashboardItem("i-1", TREND_WIDGET_ID);
        item.setIndicatorId(null);

        assertThrows(ValidationException.class, () -> service.save(authentication, trendRequest(item)));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_rejectsUnknownIndicatorId() {
        DashboardItem item = dashboardItem("i-1", "does-not-exist");

        assertThrows(ValidationException.class, () -> service.save(authentication, trendRequest(item)));
        verifyNoInteractions(resourceService);
    }

    // --- validation: year range (dashboard-level, shared across items) ---

    @Test
    void save_rejectsMissingYearTo() {
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", TREND_WIDGET_ID)), null, 2022, null, null);

        assertThrows(InvalidPresetScopeException.class, () -> service.save(authentication, request));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_rejectsMissingYearFromForTrendCapableWidget() {
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", TREND_WIDGET_ID)), null, null, 2023, null);

        assertThrows(InvalidPresetScopeException.class, () -> service.save(authentication, request));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_allowsMissingYearFromForNonTrendWidget() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", NON_TREND_WIDGET_ID)), null, null, 2023, null);

        assertDoesNotThrow(() -> service.save(authentication, request));
    }

    @Test
    void save_rejectsYearFromAfterYearTo() {
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", TREND_WIDGET_ID)), null, 2023, 2022, null);

        assertThrows(InvalidPresetScopeException.class, () -> service.save(authentication, request));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_appliesSharedYearRangeDifferentlyPerItemTimePolicy() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(
                List.of(dashboardItem("i-1", TREND_WIDGET_ID), dashboardItem("i-2", NON_TREND_WIDGET_ID)),
                null, 2022, 2023, null);

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator, times(2)).compile(captor.capture(), eq(actorContext));
        List<IndicatorQuery> queries = captor.getAllValues();
        assertNotNull(queries.get(0).timeSeries(), "trend widget gets a real timeSeries range");
        assertNull(queries.get(1).timeSeries(), "non-trend widget gets no timeSeries");
        assertTrue(queries.get(1).filters().stream()
                .anyMatch(f -> f.dimension().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION) && f.values().equals(List.of(2023))));
    }

    // --- query construction: dashboard-level filters applied to every item ---

    @Test
    void save_appliesCountryFilterWhenCountriesGiven() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", TREND_WIDGET_ID)), List.of("GR", "DE"), 2022, 2023, null);

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertTrue(captor.getValue().filters().stream().anyMatch(f -> f.dimension().equals(IndicatorCodes.COUNTRY_DIMENSION)));
    }

    @Test
    void save_derivesGroupByCountryFromThePresetsMatchingView() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        IndicatorPreset perCountryPreset = new IndicatorPreset(
                "widget-map", "Map Widget", IndicatorGroup.OPEN_ACCESS_PUBLICATIONS,
                List.of(new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.MAP)),
                false, null, List.of(), "indicator.trend", Map.of(), 30);
        lenient().when(presetCatalog.get("widget-map")).thenReturn(perCountryPreset);
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", "widget-map")), null, 2023, 2023, null);

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertEquals(List.of(IndicatorCodes.COUNTRY_DIMENSION), captor.getValue().groupBy());
    }

    @Test
    void save_derivesTheSelectorDimensionGroupByForADistributionPresetView() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        var selector = new ViewSelector(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, "Access type", List.of());
        IndicatorPreset distributionPreset = new IndicatorPreset(
                "widget-distribution", "Distribution Widget", IndicatorGroup.OPEN_ACCESS_PUBLICATIONS,
                List.of(new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.STACKED_COLUMN, selector)),
                true, null, List.of(), "indicator.distribution", Map.of(), 40);
        IndicatorDefinition distributionIndicator = indicatorDefinition("indicator.distribution", true, Set.of(
                new IndicatorDimensionBinding(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION,
                        Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP), Set.of(FilterOperator.EQ, FilterOperator.IN), false),
                new IndicatorDimensionBinding(IndicatorCodes.CLASSIFICATION_DIMENSION, Set.of(DimensionUsage.GROUP), Set.of(), false)));
        lenient().when(presetCatalog.get("widget-distribution")).thenReturn(distributionPreset);
        lenient().when(definitionCatalog.get("indicator.distribution")).thenReturn(distributionIndicator);
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", "widget-distribution")), null, 2023, 2023, null);

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertEquals(
                List.of(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, IndicatorCodes.CLASSIFICATION_DIMENSION),
                captor.getValue().groupBy());
    }

    @Test
    void save_omitsCountryFilterWhenNoCountriesGiven() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", NON_TREND_WIDGET_ID)), null, null, 2023, null);

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertTrue(captor.getValue().filters().stream().noneMatch(f -> f.dimension().equals(IndicatorCodes.COUNTRY_DIMENSION)));
    }

    @Test
    void save_nonTrendWidgetProducesSurveyYearFilterInsteadOfTimeSeries() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", NON_TREND_WIDGET_ID)), null, null, 2023, null);

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertNull(captor.getValue().timeSeries());
        assertTrue(captor.getValue().filters().stream()
                .anyMatch(f -> f.dimension().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION) && f.values().equals(List.of(2023))));
    }

    // --- validation: seriesAggregations (dashboard-level, checked per item's own allowed set) ---

    @Test
    void save_passesThroughAllowedSeriesAggregations() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", TREND_WIDGET_ID)), null, 2022, 2023, List.of(AggregationType.AVG));

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertEquals(List.of(AggregationType.AVG), captor.getValue().seriesAggregations());
    }

    @Test
    void save_rejectsSeriesAggregationNotAllowedForWidget() {
        // nonTrendDefinition.allowedSeriesAggregations() is empty for this widget
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", NON_TREND_WIDGET_ID)), null, null, 2023, List.of(AggregationType.AVG));

        assertThrows(SeriesAggregationNotAllowedException.class, () -> service.save(authentication, request));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_rejectsSharedSeriesAggregationInvalidForOneOfMultipleWidgets() {
        UserDashboard request = dashboardRequest(
                List.of(dashboardItem("i-1", TREND_WIDGET_ID), dashboardItem("i-2", NON_TREND_WIDGET_ID)),
                null, 2022, 2023, List.of(AggregationType.AVG));

        SeriesAggregationNotAllowedException ex = assertThrows(
                SeriesAggregationNotAllowedException.class, () -> service.save(authentication, request));
        assertTrue(ex.getMessage().contains(NON_TREND_WIDGET_ID));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_omittedSeriesAggregationsDefaultsToEmpty() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = dashboardRequest(List.of(dashboardItem("i-1", TREND_WIDGET_ID)), null, 2022, 2023, null);

        assertDoesNotThrow(() -> service.save(authentication, request));

        var captor = org.mockito.ArgumentCaptor.forClass(IndicatorQuery.class);
        verify(indicatorQueryValidator).compile(captor.capture(), eq(actorContext));
        assertEquals(List.of(), captor.getValue().seriesAggregations());
    }

    // --- validation: item ids ---

    @Test
    void save_rejectsDuplicateItemIds() {
        UserDashboard request = trendRequest(dashboardItem("dup", TREND_WIDGET_ID), dashboardItem("dup", TREND_WIDGET_ID));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.save(authentication, request));
        assertTrue(ex.getMessage().contains("dup"));
        verifyNoInteractions(resourceService);
    }

    @Test
    void save_allowsSameIndicatorIdTwice() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());
        UserDashboard request = trendRequest(dashboardItem("i-1", TREND_WIDGET_ID), dashboardItem("i-2", TREND_WIDGET_ID));

        service.save(authentication, request);

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        assertEquals(2, captor.getValue().getItems().size());
    }

    @Test
    void save_assignsIdsToItemsMissingIds() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());

        service.save(authentication, trendRequest(dashboardItem(null, TREND_WIDGET_ID)));

        var captor = org.mockito.ArgumentCaptor.forClass(UserDashboard.class);
        verify(service).add(captor.capture());
        String assignedId = captor.getValue().getItems().get(0).getId();
        assertNotNull(assignedId);
        assertFalse(assignedId.isBlank());
    }

    @Test
    void save_validatesEachItemsQueryThroughIndicatorQueryValidator() {
        doThrow(new ResourceNotFoundException(EMAIL, "user_dashboard")).when(service).get(EMAIL);
        doReturn(new UserDashboard()).when(service).add(any());

        service.save(authentication, trendRequest(dashboardItem("i-1", TREND_WIDGET_ID)));

        verify(indicatorQueryValidator).compile(any(IndicatorQuery.class), eq(actorContext));
    }

    // --- helpers ---

    private DashboardItem dashboardItem(String id, String indicatorId) {
        DashboardItem item = new DashboardItem();
        item.setId(id);
        item.setIndicatorId(indicatorId);
        return item;
    }

    /** Dashboard request with a default trend-widget-compatible year range (2022-2023), no other filters. */
    private UserDashboard trendRequest(DashboardItem... items) {
        return dashboardRequest(List.of(items), null, 2022, 2023, null);
    }

    private UserDashboard dashboardRequest(List<DashboardItem> items, List<String> countries, Integer yearFrom,
                                            Integer yearTo, List<AggregationType> seriesAggregations) {
        UserDashboard request = new UserDashboard();
        request.setItems(items);
        request.setCountries(countries);
        request.setYearFrom(yearFrom);
        request.setYearTo(yearTo);
        request.setSeriesAggregations(seriesAggregations);
        return request;
    }

    private IndicatorDefinition indicatorDefinition(String code, boolean timeSeriesSupported, Set<IndicatorDimensionBinding> dimensionBindings) {
        return new IndicatorDefinition(
                UUID.randomUUID(), code, "Label", "Description",
                IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.COUNT,
                IndicatorFormat.NUMBER, RenderHint.SCALAR,
                new AggregationPolicy(AggregationType.SUM, Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MEDIAN), NullHandling.EXCLUDE),
                null, dimensionBindings,
                new TimePolicy(timeSeriesSupported, timeSeriesSupported ? eu.openaire.observatory.indicator.model.MissingPeriodHandling.ZERO_FILL : eu.openaire.observatory.indicator.model.MissingPeriodHandling.OMIT),
                new IndicatorExecutionBinding("test-handler", code),
                IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1);
    }
}
