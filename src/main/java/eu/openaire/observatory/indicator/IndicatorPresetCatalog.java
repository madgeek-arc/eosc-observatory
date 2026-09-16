package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.CountryScope;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorFormat;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.model.RenderStyle;
import eu.openaire.observatory.indicator.model.TimeScope;
import eu.openaire.observatory.indicator.model.ViewSelector;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.validation.PresetScopeValidation;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static eu.openaire.observatory.indicator.IndicatorCodes.ACCESS_STATUS_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_INITIATIVE_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_POLICY_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.DATASETS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.DATASETS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.EU_COUNTRY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.FINANCIAL_INVESTMENT;
import static eu.openaire.observatory.indicator.IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.INVESTMENT_AREA_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.LONG_TERM_PRESERVATION_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.MONITORED_AREAS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.OA_LICENSE_STATUS_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.POLICY_AREAS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.RESEARCHER_POLICY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.CITIZEN_SCIENCE;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.DATA_MANAGEMENT;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.FAIR_DATA;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.INVESTMENTS_IN_EOSC;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.NATIONAL_MONITORING;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_ACCESS_PUBLICATIONS;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_DATA;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SCIENCE_POLICIES;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SCIENCE_TRAINING;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SCIENCE_TRENDS;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SOFTWARE;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.REPOSITORIES;

/** Fixed catalog of preset indicators, each an {@link IndicatorPreset} backed by an {@link IndicatorDefinition} from {@link IndicatorDefinitionCatalog}. */
@Service
public class IndicatorPresetCatalog {

    private static final Map<String, List<String>> NO_FILTERS = Map.of();
    private static final List<AggregationType> AVG_MEDIAN = List.of(AggregationType.AVG, AggregationType.MEDIAN);
    private static final List<AggregationType> NO_SERIES_AGGREGATIONS = List.of();

    /**
     * The view templates the current presets fall into, keyed by the shape of the backing
     * indicator. Each is a full {@code countryScope × timeScope} 2×2, ordered all/single,
     * all/range, selected/single, selected/range. Per-preset overrides can be introduced later
     * (e.g. the full-width "Distribution of … Types" indicators).
     */
    private static final List<PresetView> MAGNITUDE_VIEWS = List.of(
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.SCALAR),
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.LINE_CHART),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PROGRESS_BARS),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_SERIES_LINE_CHART));

    /**
     * Per-country output counts (new citizen-science projects, published software sets): the EU
     * views lean on a most-active-countries map and column charts rather than a single scalar.
     */
    private static final List<PresetView> OUTPUT_COUNT_VIEWS = List.of(
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.CHOROPLETH_MAP_WITH_TOP_5),
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COLUMN_CHART),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PROGRESS_BARS),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_SERIES_COLUMN_CHART));

    private static final List<PresetView> EU_COVERAGE_VIEWS = List.of(
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.SCALAR),
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.LINE_CHART),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.YES_NO),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.YES_NO_TIMELINE));

    private static final List<PresetView> NATIONAL_STATUS_VIEWS = List.of(
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.MAP),
        new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MAP_WITH_CHANGE_COUNT),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.YES_NO),
        new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.YES_NO_TIMELINE));

    /** The display label a gap-filled (null) year, or a country that hasn't answered yet, renders as. */
    private static final String NOT_AVAILABLE = "N/A";

    /**
     * Legend labels for the per-topic "National Monitoring on &lt;topic&gt;" presets
     * ({@code oa.country_initiative_status}): the raw {@code YES}/{@code NO}/{@code AWAITING_DATA}
     * values {@link SurveyStatsToolIndicatorQueryHandler} emits render as
     * {@code "Has national monitoring"}/{@code "Does not have national monitoring"}/{@value #NOT_AVAILABLE}.
     * Keyed on the handler's constants so a vocabulary rename is a compile error here; values it does
     * not list pass through unchanged.
     */
    private static final Map<String, String> MONITORING_STATUS_LABELS = Map.of(
        SurveyStatsToolIndicatorQueryHandler.STATUS_YES, "Has national monitoring",
        SurveyStatsToolIndicatorQueryHandler.STATUS_NO, "Does not have national monitoring",
        SurveyStatsToolIndicatorQueryHandler.STATUS_AWAITING_DATA, NOT_AVAILABLE);

    /**
     * Full-width "distribution of &lt;category&gt; by output/document type" views, shared by the
     * "Distribution of Open Access / Open Data Types" presets ({@code oa-9} / {@code od-8}).
     * A 100%-stacked column at EU/single-year (every category at once, as rendered in the legacy
     * Explore dashboard), then a year trend and per-country breakdowns. Every cell carries a
     * {@link ViewSelector} over {@code selectorDimension} so the preset-query endpoint's derivation
     * is a pure function of the matched view: it groups by {@code selectorDimension} to return one
     * series per member, and the UI shows the member the actor picked from the selector (the
     * stacked-column cell shows them all and may ignore the dropdown).
     */
    private static List<PresetView> categoryDistributionViews(String selectorDimension, String selectorLabel) {
        ViewSelector selector = new ViewSelector(selectorDimension, selectorLabel, List.of());
        return List.of(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.STACKED_COLUMN, selector),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_SERIES_LINE_CHART, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PROGRESS_LINE_CHART, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_LINE_CHARTS, selector));
    }

    /** The {@code initiativeType} "Open Science area" selector carried by every by-area pivot view. */
    private static ViewSelector openScienceAreaSelector() {
        return new ViewSelector(INITIATIVE_TYPE_DIMENSION, "Open Science area", List.of());
    }

    /**
     * Bar-chart / table / year-adopted+coverage-trend views for the "… by Areas", "… on area" and
     * "… by Countries and Areas" national policy/monitoring/financial-strategy presets. When
     * {@code selector} is non-null (the {@code initiativeType} one) the preset-query endpoint groups
     * by {@code initiativeType} so one series/row comes back per area; {@code osp-5}
     * ("Coverage of Researchers …") passes {@code null} — its backing indicator has no area
     * breakdown.
     */
    private static List<PresetView> areaTrendViews(ViewSelector selector) {
        return List.of(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.BAR_CHART, selector),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_SERIES_BAR_CHART, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.YES_NO_TABLE, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.YEAR_ADOPTED_COVERAGE_TREND, selector));
    }

    /**
     * "Number of monitored/policy areas per country" views ({@code nm-2}, {@code osp-3}). No
     * selector — {@code oa.monitored_areas_count} / {@code oa.policy_areas_count} bind only
     * {@code country} + {@code surveyYear}, so the EU/single-year
     * {@link RenderStyle#COUNT_BY_AREA_WITH_AVERAGE} "countries with Yes by area" breakdown is
     * nominal against these indicators (see {@code docs/INDICATOR_TODO.md}).
     */
    private static List<PresetView> areaCountViews() {
        return List.of(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.COUNT_BY_AREA_WITH_AVERAGE),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COLUMN_CHART_WITH_VALUE_LABELS),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PROGRESS_BARS),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COVERAGE_TREND));
    }

    /**
     * "Trend of Open Access Publications / Open Data" ({@code ost-3} / {@code ost-4}), backed by
     * {@code publications.count} / {@code datasets.count} grouped by {@code accessStatus}. Every
     * cell carries an {@code accessStatus} selector so the preset-query endpoint fans out one
     * series per access type (and adds {@code country} for the selected-countries cells).
     */
    private static List<PresetView> accessTypeTrendViews() {
        ViewSelector selector = new ViewSelector(ACCESS_STATUS_DIMENSION, "Access type", List.of());
        return List.of(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.STACKED_BAR_WITH_PROGRESS, selector),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COLUMN_CHART_WITH_VALUE_LABELS, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.MULTI_STACKED_BARS, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COMPOSITE_COLUMNS_AND_MULTI_SERIES_LINE, selector));
    }

    /** "Total amount of investments in EOSC and Open Science" ({@code inv-1}). No selector — one summed figure. */
    private static List<PresetView> totalInvestmentViews() {
        return List.of(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.SCALAR),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COLUMN_CHART),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PROGRESS_BARS),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COMPOSITE_PER_YEAR_AND_CUMULATIVE_LINES));
    }

    /**
     * "Investments in EOSC and Open Science (by Open Science area)" ({@code inv-2}). Every cell
     * carries the {@code investmentArea} selector so the preset-query endpoint groups by
     * {@code investmentArea} and one series comes back per area.
     */
    private static List<PresetView> investmentByAreaViews() {
        ViewSelector selector = new ViewSelector(INVESTMENT_AREA_DIMENSION, "Open Science area", List.of());
        return List.of(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PIE_WITH_PROGRESS_BARS, selector),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COMPOSITE_AMOUNT_AND_SHARE_COLUMNS, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.MULTI_STACKED_BARS, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.COMPOSITE_COMPOSITION_AND_PER_AREA, selector));
    }

    /** The one "*_POLICY" initiativeType member per real topic area (matches the per-topic "National Policy on …" presets). */
    private static final List<String> POLICY_AREA_TYPES = List.of(
        "OA_PUBLICATION_POLICY", "OPEN_DATA_POLICY", "FAIR_DATA_POLICY", "DATA_MANAGEMENT_POLICY",
        "CITIZEN_SCIENCE_POLICY", "REPOSITORIES_CONNECT_POLICY", "REPOSITORIES_PRESERVATION_POLICY",
        "TRAINING_POLICY", "SOFTWARE_POLICY");

    /** The one "*_MONITORING_INITIATIVE" initiativeType member per real topic area. */
    private static final List<String> MONITORING_AREA_TYPES = List.of(
        "MONITORING_INITIATIVE", "OPEN_DATA_MONITORING_INITIATIVE", "FAIR_DATA_MONITORING_INITIATIVE",
        "DATA_MANAGEMENT_MONITORING_INITIATIVE", "CITIZEN_SCIENCE_MONITORING_INITIATIVE",
        "REPOSITORIES_CONNECT_MONITORING_INITIATIVE", "REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE",
        "TRAINING_MONITORING_INITIATIVE", "SOFTWARE_MONITORING_INITIATIVE");

    /** The "*_FINANCIAL_STRATEGY" initiativeType members that map to a survey question (no preservation one exists). */
    private static final List<String> FINANCIAL_STRATEGY_AREA_TYPES = List.of(
        "FINANCIAL_STRATEGY", "OPEN_DATA_FINANCIAL_STRATEGY", "FAIR_DATA_FINANCIAL_STRATEGY",
        "DATA_MANAGEMENT_FINANCIAL_STRATEGY", "CITIZEN_SCIENCE_FINANCIAL_STRATEGY",
        "REPOSITORIES_CONNECT_FINANCIAL_STRATEGY", "TRAINING_FINANCIAL_STRATEGY", "SOFTWARE_FINANCIAL_STRATEGY");

    /** {@code getAll()} contract: preset indicators grouped by {@link IndicatorGroup#order()}, then by {@link IndicatorPreset#order()} within a group. */
    private static final Comparator<IndicatorPreset> DISPLAY_ORDER =
        Comparator.comparingInt((IndicatorPreset preset) -> preset.group().order())
            .thenComparingInt(IndicatorPreset::order);

    private final Map<String, IndicatorPreset> presetsById;
    private final List<IndicatorPreset> ordered;
    private final IndicatorDefinitionCatalog definitionCatalog;

    public IndicatorPresetCatalog(IndicatorDefinitionCatalog definitionCatalog) {
        this.definitionCatalog = definitionCatalog;
        Map<String, IndicatorPreset> byId = new LinkedHashMap<>();
        Set<String> slots = new HashSet<>();
        for (IndicatorPreset preset : allPresets()) {
            IndicatorDefinition definition = definitionCatalog.get(preset.indicatorCode());
            if (definition == null) {
                throw new IllegalStateException(
                    "Preset indicator '" + preset.id() + "' references unknown indicatorCode: "
                        + preset.indicatorCode());
            }
            if (!slots.add(preset.group().name() + '#' + preset.order())) {
                throw new IllegalStateException(
                    "Two preset indicators claim order " + preset.order() + " in group " + preset.group()
                        + " (near id '" + preset.id() + "')");
            }
            validateViews(preset, definition);
            validateValueLabels(preset, definition);
            byId.put(preset.id(), preset.resolvedAgainst(definition));
        }
        this.presetsById = Map.copyOf(byId);
        this.ordered = byId.values().stream().sorted(DISPLAY_ORDER).toList();
    }

    /** All preset indicators, grouped by topic in a fixed order and curated order within each group. */
    public List<IndicatorPreset> getAll() {
        return ordered;
    }

    public IndicatorPreset get(String id) {
        return presetsById.get(id);
    }

    /**
     * Builds the execution query for {@code preset} from the dashboard-style parameters that vary
     * at use time, applying {@link PresetScopeValidation} — the same country/year/seriesAggregation
     * rules the {@code PUT /dashboard} save path enforces.
     *
     * @throws eu.openaire.observatory.indicator.validation.SeriesAggregationNotAllowedException
     *         if a requested series aggregation is not in the preset's allowed set
     * @throws eu.openaire.observatory.indicator.validation.InvalidPresetScopeException
     *         if {@code yearTo} is null, {@code yearFrom} is null for a time-series indicator, or
     *         {@code yearFrom > yearTo}
     */
    public IndicatorQuery toIndicatorQuery(IndicatorPreset preset, List<String> countries,
            Integer yearFrom, Integer yearTo, List<AggregationType> seriesAggregations) {
        IndicatorDefinition definition = definitionCatalog.get(preset.indicatorCode());
        PresetScopeValidation.validate(
            preset, definition.timePolicy().supported(), yearFrom, yearTo, seriesAggregations);
        List<AggregationType> requested = seriesAggregations == null ? List.of() : seriesAggregations;
        return preset.toIndicatorQuery(definition, countries, yearFrom, yearTo, requested);
    }

    private static void validateViews(IndicatorPreset preset, IndicatorDefinition definition) {
        if (preset.views() == null || preset.views().isEmpty()) {
            throw new IllegalStateException("Preset indicator '" + preset.id() + "' has no views");
        }
        Set<String> cells = new HashSet<>();
        for (PresetView view : preset.views()) {
            if (!cells.add(view.countryScope() + "#" + view.timeScope())) {
                throw new IllegalStateException(
                    "Preset indicator '" + preset.id() + "' has two views for the "
                        + view.countryScope() + "/" + view.timeScope() + " cell");
            }
            validateSelector(preset, definition, view.selector());
        }
    }

    /**
     * A preset that declares legend labels ({@link IndicatorPreset#labels()}, applied by
     * {@link PresetValueLabeler}) must back a per-country status indicator — {@code YES_NO} or
     * {@code CATEGORY} format. Blank entries are rejected by {@link PresetValueLabels} itself;
     * whether the map covers every value the handler can emit is not checked: those are runtime data.
     */
    static void validateValueLabels(IndicatorPreset preset, IndicatorDefinition definition) {
        if (preset.labels().isEmpty()) {
            return;
        }
        if (definition.format() != IndicatorFormat.YES_NO && definition.format() != IndicatorFormat.CATEGORY) {
            throw new IllegalStateException(
                "Preset indicator '" + preset.id() + "' declares value labels but its format is "
                    + definition.format() + " (only YES_NO / CATEGORY carry per-country status values)");
        }
    }

    private static void validateSelector(IndicatorPreset preset, IndicatorDefinition definition, ViewSelector selector) {
        if (selector == null) {
            return;
        }
        boolean groupable = definition.dimensionBindings().stream()
            .anyMatch(binding -> binding.dimensionCode().equals(selector.dimension())
                && binding.usages().contains(DimensionUsage.GROUP));
        if (!groupable) {
            throw new IllegalStateException(
                "Preset indicator '" + preset.id() + "' has a view selector on dimension '"
                    + selector.dimension() + "' which " + preset.indicatorCode()
                    + " does not bind for grouping");
        }
    }

    private static List<IndicatorPreset> allPresets() {
        // Ordering follows the "EOSC_OS_Observatory_Indicator_search (Indicator Inventory)" CSV, per
        // group. Presets that are not in that inventory (the "National Financial Strategy on …"
        // status-map variants, all id-suffixed "-x") are kept, but ordered after every inventory row
        // in their group.
        return List.of(
            // ---- Open Access Publications (inventory rows 2-10) --------------------------------------
            preset("oa-1", "OA Publication in Europe (OA vs Closed)", OPEN_ACCESS_PUBLICATIONS,
                MAGNITUDE_VIEWS, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA"), 10),
            preset("oa-2", "EU Countries with Policy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OA_PUBLICATION_POLICY"), 20),
            preset("oa-3", "Financial investments in OA Publication", OPEN_ACCESS_PUBLICATIONS,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OA_PUBLICATION"), 30),
            preset("oa-4", "EU countries with Policy on Immediate OA Publications", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("IMMEDIATE_OA_POLICY"), 40),
            preset("oa-5", "EU Countries with Financial Strategy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FINANCIAL_STRATEGY"), 50),
            preset("oa-6", "EU Countries with National OA Publication Monitoring Initiatives", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("MONITORING_INITIATIVE"), 60),
            nationalPolicyPreset("oa-7", "National Policy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                "OA_PUBLICATION_POLICY", 70),
            nationalMonitoringPreset("oa-8", "National Monitoring on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                "MONITORING_INITIATIVE", 80),
            presetFullWidth("oa-9", "Distribution of Open Access Types by Different Scholarly Publication Outputs",
                OPEN_ACCESS_PUBLICATIONS, categoryDistributionViews(OA_LICENSE_STATUS_DIMENSION, "Access type"),
                NO_SERIES_AGGREGATIONS, PUBLICATIONS_OA_LICENSE_STATUS_COUNT, NO_FILTERS, 90),

            // ---- Open Data (inventory rows 11-18) -------------------------------------------------------
            preset("od-1", "Open Data in Europe (Open vs Closed)", OPEN_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA"), 10),
            preset("od-2", "EU Countries with Policy on Open Data", OPEN_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_POLICY"), 20),
            preset("od-3", "Financial investments in Open Data", OPEN_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OPEN_DATA"), 30),
            preset("od-4", "EU Countries with Financial Strategy on Open Data", OPEN_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY"), 40),
            preset("od-5", "EU Countries with National Open Data Monitoring Initiatives", OPEN_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_MONITORING_INITIATIVE"), 50),
            nationalPolicyPreset("od-6", "National Policy on Open Data", OPEN_DATA,
                "OPEN_DATA_POLICY", 60),
            nationalMonitoringPreset("od-7", "National Monitoring on Open Data", OPEN_DATA,
                "OPEN_DATA_MONITORING_INITIATIVE", 70),
            presetFullWidth("od-8", "Distribution of Open Data Types by Different Document Type",
                OPEN_DATA, categoryDistributionViews(ACCESS_STATUS_DIMENSION, "Access type"),
                NO_SERIES_AGGREGATIONS, DATASETS_COUNT, NO_FILTERS, 80),

            // ---- FAIR Data (inventory rows 19-24) -----------------------------------------------------
            preset("fair-1", "EU Countries with Policy on FAIR Data", FAIR_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_POLICY"), 10),
            preset("fair-2", "Financial investments in FAIR Data", FAIR_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("FAIR_DATA"), 20),
            preset("fair-3", "EU Countries with Financial Strategy on FAIR Data", FAIR_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY"), 30),
            preset("fair-4", "EU Countries with National FAIR Data Monitoring Initiatives", FAIR_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_MONITORING_INITIATIVE"), 40),
            nationalPolicyPreset("fair-5", "National Policy on FAIR Data", FAIR_DATA,
                "FAIR_DATA_POLICY", 50),
            nationalMonitoringPreset("fair-6", "National Monitoring on FAIR Data", FAIR_DATA,
                "FAIR_DATA_MONITORING_INITIATIVE", 60),

            // ---- Data Management (inventory rows 25-30) ---------------------------------------------
            preset("dm-1", "EU Countries with Policy on Data Management", DATA_MANAGEMENT,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_POLICY"), 10),
            preset("dm-2", "Financial investments in Data Management", DATA_MANAGEMENT,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("DATA_MANAGEMENT"), 20),
            preset("dm-3", "EU Countries with Financial Strategy on Data Management", DATA_MANAGEMENT,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY"), 30),
            preset("dm-4", "EU Countries with National Data Management Monitoring Initiatives", DATA_MANAGEMENT,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE"), 40),
            nationalPolicyPreset("dm-5", "National Policy on Data Management", DATA_MANAGEMENT,
                "DATA_MANAGEMENT_POLICY", 50),
            nationalMonitoringPreset("dm-6", "National Monitoring on Data Management", DATA_MANAGEMENT,
                "DATA_MANAGEMENT_MONITORING_INITIATIVE", 60),

            // ---- Citizen Science (inventory rows 31-37) -------------------------------------------------
            presetFullWidth("cs-1", "New Citizen Science projects in EU Countries", CITIZEN_SCIENCE,
                OUTPUT_COUNT_VIEWS, AVG_MEDIAN, CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, NO_FILTERS, 10),
            preset("cs-2", "EU Countries with Policy on Citizen Science", CITIZEN_SCIENCE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_POLICY"), 20),
            preset("cs-3", "Financial investments in Citizen Science", CITIZEN_SCIENCE,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("CITIZEN_SCIENCE"), 30),
            preset("cs-4", "EU Countries with Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY"), 40),
            preset("cs-5", "EU Countries with Citizen Science Monitoring Initiatives", CITIZEN_SCIENCE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE"), 50),
            nationalPolicyPreset("cs-6", "National Policy on Citizen Science", CITIZEN_SCIENCE,
                "CITIZEN_SCIENCE_POLICY", 60),
            nationalMonitoringPreset("cs-7", "National Monitoring on Citizen Science", CITIZEN_SCIENCE,
                "CITIZEN_SCIENCE_MONITORING_INITIATIVE", 70),

            // ---- Repositories (inventory rows 38-46; covers both Connecting Repositories to EOSC and
            //      Long-term Data Preservation sub-topics) --------------------------------------------
            preset("repo-1", "Repositories offering long-term preservation in EU Countries", REPOSITORIES,
                MAGNITUDE_VIEWS, AVG_MEDIAN, LONG_TERM_PRESERVATION_COUNT, NO_FILTERS, 10),
            preset("repo-2", "EU Countries with Policy on Connecting Repositories to EOSC", REPOSITORIES,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_POLICY"), 20),
            preset("repo-3", "Financial investments in connecting repositories to EOSC", REPOSITORIES,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("REPOSITORIES_CONNECT"), 30),
            preset("repo-4", "EU Countries with Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY"), 40),
            preset("repo-5", "EU Countries with Monitoring Initiatives on Connecting Repositories to EOSC", REPOSITORIES,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE"), 50),
            nationalPolicyPreset("repo-6", "National Policy on Connecting Repositories to EOSC", REPOSITORIES,
                "REPOSITORIES_CONNECT_POLICY", 60),
            nationalMonitoringPreset("repo-7", "National Monitoring on Connecting Repositories to EOSC", REPOSITORIES,
                "REPOSITORIES_CONNECT_MONITORING_INITIATIVE", 70),
            nationalPolicyPreset("repo-8", "National Policy on Long-term Data Preservation", REPOSITORIES,
                "REPOSITORIES_PRESERVATION_POLICY", 80),
            nationalMonitoringPreset("repo-9", "National Monitoring on Long-term Data Preservation", REPOSITORIES,
                "REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE", 90),

            // ---- Open Science Training (inventory rows 47-52) ----------------------------------------
            preset("train-1", "EU Countries with Policy on skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_POLICY"), 10),
            preset("train-2", "Financial investments in skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("TRAINING"), 20),
            preset("train-3", "EU Countries with Financial Strategy on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_FINANCIAL_STRATEGY"), 30),
            preset("train-4", "EU Countries with Monitoring Initiatives on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_MONITORING_INITIATIVE"), 40),
            nationalPolicyPreset("train-5", "National Policy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                "TRAINING_POLICY", 50),
            nationalMonitoringPreset("train-6", "National Monitoring on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                "TRAINING_MONITORING_INITIATIVE", 60),

            // ---- Open Software (inventory rows 53-59) -----------------------------------------------
            presetFullWidth("sw-1", "Open Software sets published in EU Countries", OPEN_SOFTWARE,
                OUTPUT_COUNT_VIEWS, AVG_MEDIAN, SOFTWARE_OPEN_SOURCE_SETS_COUNT, NO_FILTERS, 10),
            preset("sw-2", "EU Countries with Policy on Open Software", OPEN_SOFTWARE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_POLICY"), 20),
            preset("sw-3", "Financial investments in Open Software", OPEN_SOFTWARE,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("SOFTWARE"), 30),
            preset("sw-4", "EU Countries with Financial Strategy on Open Software", OPEN_SOFTWARE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_FINANCIAL_STRATEGY"), 40),
            preset("sw-5", "EU Countries with Monitoring Initiatives on Open Software", OPEN_SOFTWARE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_MONITORING_INITIATIVE"), 50),
            nationalPolicyPreset("sw-6", "National Policy on Open Source Software", OPEN_SOFTWARE,
                "SOFTWARE_POLICY", 60),
            nationalMonitoringPreset("sw-7", "National Monitoring on Open Source Software", OPEN_SOFTWARE,
                "SOFTWARE_MONITORING_INITIATIVE", 70),

            // ---- Investments in EOSC page (inventory rows 64-65) -----------------------------------
            presetFullWidth("inv-1", "Total amount of investments in EOSC and Open Science", INVESTMENTS_IN_EOSC,
                totalInvestmentViews(), AVG_MEDIAN, FINANCIAL_INVESTMENT, NO_FILTERS, 10),
            // Same oa.financial_investment indicator as inv-1, broken out per investment area — the
            // definition is groupable by investmentArea (see IndicatorDefinitionCatalog.financialInvestment).
            presetFullWidth("inv-2", "Investments in EOSC and Open Science (by Open Science area)", INVESTMENTS_IN_EOSC,
                investmentByAreaViews(), AVG_MEDIAN, FINANCIAL_INVESTMENT, NO_FILTERS, 20),

            // ---- Open Science Trends (inventory rows 60-63) ----------------------------------------
            // The "… by Areas" rows are eu_country_coverage pinned to every topic's *_POLICY /
            // *_FINANCIAL_STRATEGY member (an IN filter); the initiativeType selector on each view
            // makes the preset-query endpoint group by initiativeType, so one series comes back per area.
            presetFullWidth("ost-1", "Trends in National Open Science Policies by Areas", OPEN_SCIENCE_TRENDS,
                areaTrendViews(openScienceAreaSelector()),
                NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeTypes(POLICY_AREA_TYPES), 10),
            presetFullWidth("ost-2", "Trends in Financial Strategy on EOSC and Open Science by Areas", OPEN_SCIENCE_TRENDS,
                areaTrendViews(openScienceAreaSelector()),
                NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeTypes(FINANCIAL_STRATEGY_AREA_TYPES), 20),
            // ost-3/ost-4 back on publications.count / datasets.count grouped by accessStatus (the
            // *_access_status_share indicators can't fan out per access type — accessStatus is a
            // required filter-only binding there). Every view carries an accessStatus selector.
            preset("ost-3", "Trend of Open Access Publications", OPEN_SCIENCE_TRENDS,
                accessTypeTrendViews(), AVG_MEDIAN, PUBLICATIONS_COUNT, NO_FILTERS, 30),
            presetFullWidth("ost-4", "Trend of Open Data", OPEN_SCIENCE_TRENDS,
                accessTypeTrendViews(), AVG_MEDIAN, DATASETS_COUNT, NO_FILTERS, 40),

            // ---- National Monitoring (inventory rows 66-69, merged into 2 presets) ---------------
            presetFullWidth("nm-1", "National Monitoring of Open Science by Areas", NATIONAL_MONITORING,
                areaTrendViews(openScienceAreaSelector()),
                NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeTypes(MONITORING_AREA_TYPES), 10),
            presetFullWidth("nm-2", "National Monitoring (number of monitored areas per country)", NATIONAL_MONITORING,
                areaCountViews(), NO_SERIES_AGGREGATIONS, MONITORED_AREAS_COUNT, NO_FILTERS, 20),

            // ---- Open Science Policies (inventory rows 70-75) ----------------------------------------
            presetFullWidth("osp-1", "National Open Science Policies by Areas", OPEN_SCIENCE_POLICIES,
                areaTrendViews(openScienceAreaSelector()),
                NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeTypes(POLICY_AREA_TYPES), 10),
            presetFullWidth("osp-2", "Financial Strategy on EOSC and Open Science by Areas", OPEN_SCIENCE_POLICIES,
                areaTrendViews(openScienceAreaSelector()),
                NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeTypes(FINANCIAL_STRATEGY_AREA_TYPES), 20),
            presetFullWidth("osp-3", "National Policies (number of policy areas per country)", OPEN_SCIENCE_POLICIES,
                areaCountViews(), NO_SERIES_AGGREGATIONS, POLICY_AREAS_COUNT, NO_FILTERS, 30),
            presetFullWidth("osp-4", "National Policy on area", OPEN_SCIENCE_POLICIES,
                areaTrendViews(openScienceAreaSelector()), NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS,
                initiativeType("OA_PUBLICATION_POLICY"), 40)
                .withValueLabels(PresetValueLabels.forMissing(NOT_AVAILABLE)),
            // Registered for catalog completeness — not queryable yet, returns HTTP 501 (see
            // IndicatorDefinitionCatalog.researcherPolicyCoverage() / docs/INDICATOR_TODO.md).
            // researcher_policy_coverage has no initiativeType binding, so areaTrendViews takes no selector.
            presetFullWidth("osp-5", "Coverage of Researchers by National Open Science Policies in Europe", OPEN_SCIENCE_POLICIES,
                areaTrendViews(null), NO_SERIES_AGGREGATIONS, RESEARCHER_POLICY_COVERAGE, NO_FILTERS, 50),
            presetFullWidth("osp-6", "National Open Science Policies by Countries and Areas", OPEN_SCIENCE_POLICIES,
                areaTrendViews(openScienceAreaSelector()), NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS,
                initiativeTypes(POLICY_AREA_TYPES), 60)
                .withValueLabels(PresetValueLabels.forMissing(NOT_AVAILABLE))
        );
    }

    private static IndicatorPreset preset(
            String id, String label, IndicatorGroup group, List<PresetView> views,
            List<AggregationType> allowedSeriesAggregations, String indicatorCode,
            Map<String, List<String>> presetFilters, int order) {
        return preset(id, label, group, views, false, allowedSeriesAggregations, indicatorCode, presetFilters, order);
    }

    /** Like {@link #preset}, but for the handful of presets whose views need the whole dashboard width. */
    private static IndicatorPreset presetFullWidth(
            String id, String label, IndicatorGroup group, List<PresetView> views,
            List<AggregationType> allowedSeriesAggregations, String indicatorCode,
            Map<String, List<String>> presetFilters, int order) {
        return preset(id, label, group, views, true, allowedSeriesAggregations, indicatorCode, presetFilters, order);
    }

    private static IndicatorPreset preset(
            String id, String label, IndicatorGroup group, List<PresetView> views, boolean fullWidth,
            List<AggregationType> allowedSeriesAggregations, String indicatorCode,
            Map<String, List<String>> presetFilters, int order) {
        // format is null here — IndicatorPresetCatalog fills it from the linked definition.
        return new IndicatorPreset(id, label, group, views, fullWidth, null,
            allowedSeriesAggregations, indicatorCode, presetFilters, PresetValueLabels.NONE, order);
    }

    /**
     * "National Policy on &lt;topic&gt;" preset: national-status views over
     * {@code oa.country_policy_status}, pinned to the {@code *_POLICY} {@code initiativeType}
     * member {@code policyType}. Its raw values are display-ready, so only the gap-filled
     * {@code null} is relabeled (to {@value #NOT_AVAILABLE}).
     */
    private static IndicatorPreset nationalPolicyPreset(
            String id, String label, IndicatorGroup group, String policyType, int order) {
        return preset(id, label, group, NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS,
                COUNTRY_POLICY_STATUS, initiativeType(policyType), order)
            .withValueLabels(PresetValueLabels.forMissing(NOT_AVAILABLE));
    }

    /**
     * "National Monitoring on &lt;topic&gt;" preset: national-status views over
     * {@code oa.country_initiative_status}, pinned to the {@code *_MONITORING_INITIATIVE}
     * {@code initiativeType} member {@code monitoringType}. Its raw
     * {@code YES}/{@code NO}/{@code AWAITING_DATA} values (and the gap-filled {@code null}) map to
     * the {@link #MONITORING_STATUS_LABELS} legend labels.
     */
    private static IndicatorPreset nationalMonitoringPreset(
            String id, String label, IndicatorGroup group, String monitoringType, int order) {
        return preset(id, label, group, NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS,
                COUNTRY_INITIATIVE_STATUS, initiativeType(monitoringType), order)
            .withValueLabels(new PresetValueLabels(MONITORING_STATUS_LABELS, NOT_AVAILABLE));
    }

    private static Map<String, List<String>> accessStatus(String value) {
        return Map.of(ACCESS_STATUS_DIMENSION, List.of(value));
    }

    private static Map<String, List<String>> initiativeType(String value) {
        return Map.of(INITIATIVE_TYPE_DIMENSION, List.of(value));
    }

    /** A multi-value {@code initiativeType} preset filter (becomes an IN filter) — for the "… by Areas" pivots. */
    private static Map<String, List<String>> initiativeTypes(List<String> values) {
        return Map.of(INITIATIVE_TYPE_DIMENSION, values);
    }

    private static Map<String, List<String>> investmentArea(String value) {
        return Map.of(INVESTMENT_AREA_DIMENSION, List.of(value));
    }
}
