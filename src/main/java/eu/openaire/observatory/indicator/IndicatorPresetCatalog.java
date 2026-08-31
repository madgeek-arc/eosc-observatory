package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.CountryScope;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.model.RenderStyle;
import eu.openaire.observatory.indicator.model.TimeScope;
import eu.openaire.observatory.indicator.model.ViewSelector;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
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
import static eu.openaire.observatory.indicator.IndicatorCodes.EU_COUNTRY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.FINANCIAL_INVESTMENT;
import static eu.openaire.observatory.indicator.IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.INVESTMENT_AREA_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.LONG_TERM_PRESERVATION_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.CITIZEN_SCIENCE;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.DATA_MANAGEMENT;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.FAIR_DATA;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_ACCESS_PUBLICATIONS;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_DATA;
import static eu.openaire.observatory.indicator.model.IndicatorGroup.OPEN_SCIENCE_TRAINING;
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
        return List.of(
            // ---- Open Access Publications --------------------------------------------------------
            preset("oa-1", "OA Publication in Europe (OA vs Closed)", OPEN_ACCESS_PUBLICATIONS,
                MAGNITUDE_VIEWS, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA"), 10),
            preset("oa-2", "Trend of Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                MAGNITUDE_VIEWS, AVG_MEDIAN, PUBLICATIONS_ACCESS_STATUS_SHARE, accessStatus("OA"), 20),
            preset("oa-3", "EU Countries with Policy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OA_PUBLICATION_POLICY"), 30),
            preset("oa-4", "National Policy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("OA_PUBLICATION_POLICY"), 40),
            preset("oa-5", "EU Countries with National OA Publication Monitoring Initiatives", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("MONITORING_INITIATIVE"), 50),
            preset("oa-6", "National Monitoring on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("MONITORING_INITIATIVE"), 60),
            preset("oa-7", "EU Countries with Financial Strategy on OA Publication", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FINANCIAL_STRATEGY"), 70),
            preset("oa-8-x", "National Financial Strategy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FINANCIAL_STRATEGY"), 80),
            preset("oa-9", "EU countries with Policy on Immediate OA Publications", OPEN_ACCESS_PUBLICATIONS,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("IMMEDIATE_OA_POLICY"), 90),
            preset("oa-10", "Financial investments in OA Publication", OPEN_ACCESS_PUBLICATIONS,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OA_PUBLICATION"), 100),

            // ---- Open Data -------------------------------------------------------------------------
            preset("od-1", "Open Data in Europe (Open vs Closed)", OPEN_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA"), 10),
            preset("od-2", "Trend of Open Data", OPEN_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, DATASETS_ACCESS_STATUS_SHARE, accessStatus("OA"), 20),
            preset("od-3", "EU Countries with Policy on Open Data", OPEN_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_POLICY"), 30),
            preset("od-4", "National Policy on Open Data", OPEN_DATA,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("OPEN_DATA_POLICY"), 40),
            preset("od-5", "EU Countries with National Open Data Monitoring Initiatives", OPEN_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_MONITORING_INITIATIVE"), 50),
            preset("od-6", "National Monitoring on Open Data", OPEN_DATA,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_MONITORING_INITIATIVE"), 60),
            preset("od-7", "EU Countries with Financial Strategy on Open Data", OPEN_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY"), 70),
            preset("od-8-x", "National Financial Strategy on Open Data", OPEN_DATA,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("OPEN_DATA_FINANCIAL_STRATEGY"), 80),
            preset("od-9", "Financial investments in Open Data", OPEN_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("OPEN_DATA"), 90),

            // ---- FAIR Data -------------------------------------------------------------------------
            preset("fair-1", "EU Countries with Policy on FAIR Data", FAIR_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_POLICY"), 10),
            preset("fair-2", "National Policy on FAIR Data", FAIR_DATA,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("FAIR_DATA_POLICY"), 20),
            preset("fair-3", "EU Countries with National FAIR Data Monitoring Initiatives", FAIR_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_MONITORING_INITIATIVE"), 30),
            preset("fair-4", "National Monitoring on FAIR Data", FAIR_DATA,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_MONITORING_INITIATIVE"), 40),
            preset("fair-5", "EU Countries with Financial Strategy on FAIR Data", FAIR_DATA,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY"), 50),
            preset("fair-6-x", "National Financial Strategy on FAIR Data", FAIR_DATA,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("FAIR_DATA_FINANCIAL_STRATEGY"), 60),
            preset("fair-7", "Financial investments in FAIR Data", FAIR_DATA,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("FAIR_DATA"), 70),

            // ---- Data Management --------------------------------------------------------------------
            preset("dm-1", "EU Countries with Policy on Data Management", DATA_MANAGEMENT,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_POLICY"), 10),
            preset("dm-2", "National Policy on Data Management", DATA_MANAGEMENT,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("DATA_MANAGEMENT_POLICY"), 20),
            preset("dm-3", "EU Countries with National Data Management Monitoring Initiatives", DATA_MANAGEMENT,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE"), 30),
            preset("dm-4", "National Monitoring on Data Management", DATA_MANAGEMENT,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE"), 40),
            preset("dm-5", "EU Countries with Financial Strategy on Data Management", DATA_MANAGEMENT,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY"), 50),
            preset("dm-6-x", "National Financial Strategy on Data Management", DATA_MANAGEMENT,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY"), 60),
            preset("dm-7", "Financial investments in Data Management", DATA_MANAGEMENT,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("DATA_MANAGEMENT"), 70),

            // ---- Citizen Science ---------------------------------------------------------------------
            preset("cs-1", "New Citizen Science projects in EU Countries", CITIZEN_SCIENCE,
                OUTPUT_COUNT_VIEWS, AVG_MEDIAN, CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, NO_FILTERS, 10),
            preset("cs-2", "EU Countries with Policy on Citizen Science", CITIZEN_SCIENCE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_POLICY"), 20),
            preset("cs-3", "National Policy on Citizen Science", CITIZEN_SCIENCE,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("CITIZEN_SCIENCE_POLICY"), 30),
            preset("cs-4", "EU Countries with Citizen Science Monitoring Initiatives", CITIZEN_SCIENCE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE"), 40),
            preset("cs-5", "National Monitoring on Citizen Science", CITIZEN_SCIENCE,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE"), 50),
            preset("cs-6", "EU Countries with Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY"), 60),
            preset("cs-7-x", "National Financial Strategy on Citizen Science", CITIZEN_SCIENCE,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY"), 70),
            preset("cs-8", "Financial investments in Citizen Science", CITIZEN_SCIENCE,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("CITIZEN_SCIENCE"), 80),

            // ---- Repositories (covers both Connecting Repositories to EOSC and Long-term Data
            //      Preservation sub-topics) ------------------------------------------------------------
            preset("repo-1", "Repositories offering long-term preservation in EU Countries", REPOSITORIES,
                MAGNITUDE_VIEWS, AVG_MEDIAN, LONG_TERM_PRESERVATION_COUNT, NO_FILTERS, 10),
            preset("repo-2", "EU Countries with Policy on Connecting Repositories to EOSC", REPOSITORIES,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_POLICY"), 20),
            preset("repo-3", "National Policy on Connecting Repositories to EOSC", REPOSITORIES,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_CONNECT_POLICY"), 30),
            preset("repo-4", "EU Countries with Monitoring Initiatives on Connecting Repositories to EOSC", REPOSITORIES,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE"), 40),
            preset("repo-5", "National Monitoring on Connecting Repositories to EOSC", REPOSITORIES,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE"), 50),
            preset("repo-6", "EU Countries with Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY"), 60),
            preset("repo-7-x", "National Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY"), 70),
            preset("repo-8", "Financial investments in connecting repositories to EOSC", REPOSITORIES,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("REPOSITORIES_CONNECT"), 80),
            preset("repo-9", "National Policy on Long-term Data Preservation", REPOSITORIES,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("REPOSITORIES_PRESERVATION_POLICY"), 90),
            preset("repo-10", "National Monitoring on Long-term Data Preservation", REPOSITORIES,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE"), 100),
            // No REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY/REPOSITORIES_PRESERVATION member exists yet,
            // so the preservation financial-strategy/investment presets are omitted.

            // ---- Open Science Training -----------------------------------------------------------
            preset("train-1", "EU Countries with Policy on skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_POLICY"), 10),
            preset("train-2", "National Policy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("TRAINING_POLICY"), 20),
            preset("train-3", "EU Countries with Monitoring Initiatives on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_MONITORING_INITIATIVE"), 30),
            preset("train-4", "National Monitoring on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_MONITORING_INITIATIVE"), 40),
            preset("train-5", "EU Countries with Financial Strategy on skills/training in Open Science", OPEN_SCIENCE_TRAINING,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("TRAINING_FINANCIAL_STRATEGY"), 50),
            preset("train-6-x", "National Financial Strategy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("TRAINING_FINANCIAL_STRATEGY"), 60),
            preset("train-7", "Financial investments in skills/training for Open Science", OPEN_SCIENCE_TRAINING,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("TRAINING"), 70),

            // ---- Open Software ----------------------------------------------------------------------
            preset("sw-1", "Open Software sets published in EU Countries", OPEN_SOFTWARE,
                OUTPUT_COUNT_VIEWS, AVG_MEDIAN, SOFTWARE_OPEN_SOURCE_SETS_COUNT, NO_FILTERS, 10),
            preset("sw-2", "EU Countries with Policy on Open Software", OPEN_SOFTWARE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_POLICY"), 20),
            preset("sw-3", "National Policy on Open Source Software", OPEN_SOFTWARE,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_POLICY_STATUS, initiativeType("SOFTWARE_POLICY"), 30),
            preset("sw-4", "EU Countries with Monitoring Initiatives on Open Software", OPEN_SOFTWARE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_MONITORING_INITIATIVE"), 40),
            preset("sw-5", "National Monitoring on Open Source Software", OPEN_SOFTWARE,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_MONITORING_INITIATIVE"), 50),
            preset("sw-6", "EU Countries with Financial Strategy on Open Software", OPEN_SOFTWARE,
                EU_COVERAGE_VIEWS, NO_SERIES_AGGREGATIONS, EU_COUNTRY_COVERAGE, initiativeType("SOFTWARE_FINANCIAL_STRATEGY"), 60),
            preset("sw-7-x", "National Financial Strategy on Open Software", OPEN_SOFTWARE,
                NATIONAL_STATUS_VIEWS, NO_SERIES_AGGREGATIONS, COUNTRY_INITIATIVE_STATUS, initiativeType("SOFTWARE_FINANCIAL_STRATEGY"), 70),
            preset("sw-8", "Financial investments in Open Software", OPEN_SOFTWARE,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, investmentArea("SOFTWARE"), 80),

            // ---- Financial Investment (Investments in EOSC page) -------------------------------------
            preset("fin-1", "Total amount of investments in EOSC and Open Science", IndicatorGroup.FINANCIAL_INVESTMENT,
                MAGNITUDE_VIEWS, AVG_MEDIAN, FINANCIAL_INVESTMENT, NO_FILTERS, 10)
        );
    }

    private static IndicatorPreset preset(
            String id, String label, IndicatorGroup group, List<PresetView> views,
            List<AggregationType> allowedSeriesAggregations, String indicatorCode,
            Map<String, List<String>> presetFilters, int order) {
        // format is null here — IndicatorPresetCatalog fills it from the linked definition.
        return new IndicatorPreset(id, label, group, views, null,
            allowedSeriesAggregations, indicatorCode, presetFilters, order);
    }

    private static Map<String, List<String>> accessStatus(String value) {
        return Map.of(ACCESS_STATUS_DIMENSION, List.of(value));
    }

    private static Map<String, List<String>> initiativeType(String value) {
        return Map.of(INITIATIVE_TYPE_DIMENSION, List.of(value));
    }

    private static Map<String, List<String>> investmentArea(String value) {
        return Map.of(INVESTMENT_AREA_DIMENSION, List.of(value));
    }
}
