package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.CountryScope;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorFormat;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.model.RenderStyle;
import eu.openaire.observatory.indicator.model.RenderStyleOption;
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

    private static final List<AggregationType> AVG_MEDIAN = List.of(AggregationType.AVG, AggregationType.MEDIAN);

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

    /** Like OUTPUT_COUNT_VIEWS, but only the EU map view is full-width. */
    private static final List<PresetView> OUTPUT_COUNT_VIEWS_MAP_FULL_WIDTH = List.of(
        OUTPUT_COUNT_VIEWS.get(0).withFullWidth(true),
        OUTPUT_COUNT_VIEWS.get(1),
        OUTPUT_COUNT_VIEWS.get(2),
        OUTPUT_COUNT_VIEWS.get(3));

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
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.STACKED_COLUMN, null),
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_SERIES_LINE_CHART, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.PROGRESS_LINE_CHART, selector),
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_LINE_CHARTS, selector));
    }

    /** The {@code initiativeType} "Open Science area" selector carried by every by-area pivot view. */
    private static ViewSelector openScienceAreaSelector() {
        return new ViewSelector(INITIATIVE_TYPE_DIMENSION, "Open Science area", List.of());
    }

    /**
     * The SELECTED_COUNTRIES/TIME_RANGE switcher every {@link #areaTrendViews} preset offers: the
     * year each country adopted (with the {@code to}-year status alongside), the coverage trend
     * alone, or a per-country yes/no snapshot of the latest year.
     */
    private static final List<RenderStyleOption> AREA_TREND_TIME_RANGE_STYLES = List.of(
        new RenderStyleOption(RenderStyle.YEAR_ADOPTED_TABLE, "Year adopted"),
        new RenderStyleOption(RenderStyle.COVERAGE_TREND, "Coverage trend"),
        new RenderStyleOption(RenderStyle.LATEST_YEAR_YES_NO_TABLE, "Latest year"));

    /** Like {@link #AREA_TREND_TIME_RANGE_STYLES}, but without the "Coverage trend" option — nm-1 only. */
    private static final List<RenderStyleOption> AREA_TREND_TIME_RANGE_STYLES_WITHOUT_COVERAGE_TREND =
        AREA_TREND_TIME_RANGE_STYLES.stream()
            .filter(option -> option.style() != RenderStyle.COVERAGE_TREND)
            .toList();

    /**
     * Replaces the SELECTED_COUNTRIES/TIME_RANGE cell of {@code views} with a switcher over
     * {@code renderStyles} — same cell, same query, several renderings the UI toggles between.
     * Every other cell in {@code views} passes through unchanged.
     */
    private static List<PresetView> withTimeRangeSwitcher(List<PresetView> views, List<RenderStyleOption> renderStyles) {
        return views.stream()
            .map(view -> view.countryScope() == CountryScope.SELECTED_COUNTRIES && view.timeScope() == TimeScope.TIME_RANGE
                ? view.withRenderStyles(renderStyles)
                : view)
            .toList();
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
            new PresetView(CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.YEAR_ADOPTED_TABLE, selector));
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
            validateRenderStyles(preset, view);
        }
    }

    /**
     * A view offering more than one switchable render style must give every option a caption
     * (the switcher's tab/button label) and must not mix styles that disagree on
     * {@link RenderStyle#perCountryBreakdown()} — {@link IndicatorPreset#toIndicatorQuery} derives
     * {@code groupBy} once per cell from the first style, so every style in the switcher has to
     * agree on whether the shared response needs a per-country breakdown.
     */
    private static void validateRenderStyles(IndicatorPreset preset, PresetView view) {
        List<RenderStyleOption> styles = view.renderStyles();
        if (styles.size() < 2) {
            return;
        }
        if (styles.stream().map(RenderStyleOption::style).distinct().count() != styles.size()) {
            throw new IllegalStateException(
                "Preset indicator '" + preset.id() + "' repeats a render style in its "
                    + view.countryScope() + "/" + view.timeScope() + " switcher");
        }
        if (styles.stream().anyMatch(option -> option.label() == null || option.label().isBlank())) {
            throw new IllegalStateException(
                "Preset indicator '" + preset.id() + "' has a " + view.countryScope() + "/" + view.timeScope()
                    + " switcher where not every render style carries a label");
        }
        if (styles.stream().map(option -> option.style().perCountryBreakdown()).distinct().count() > 1) {
            throw new IllegalStateException(
                "Preset indicator '" + preset.id() + "' has a " + view.countryScope() + "/" + view.timeScope()
                    + " switcher whose render styles disagree on perCountryBreakdown()");
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
            IndicatorPreset.builder("oa-1", "OA Publication in Europe (OA vs Closed)", OPEN_ACCESS_PUBLICATIONS)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(PUBLICATIONS_ACCESS_STATUS_SHARE).presetFilters(accessStatus("OA")).order(10).build(),
            IndicatorPreset.builder("oa-2", "EU Countries with Policy on OA Publication", OPEN_ACCESS_PUBLICATIONS)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("OA_PUBLICATION_POLICY")).order(20).build(),
            IndicatorPreset.builder("oa-3", "Financial investments in OA Publication", OPEN_ACCESS_PUBLICATIONS)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("OA_PUBLICATION")).order(30).build(),
            IndicatorPreset.builder("oa-4", "EU countries with Policy on Immediate OA Publications", OPEN_ACCESS_PUBLICATIONS)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("IMMEDIATE_OA_POLICY")).order(40).build(),
            IndicatorPreset.builder("oa-5", "EU Countries with Financial Strategy on OA Publication", OPEN_ACCESS_PUBLICATIONS)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("FINANCIAL_STRATEGY")).order(50).build(),
            IndicatorPreset.builder("oa-6", "EU Countries with National OA Publication Monitoring Initiatives", OPEN_ACCESS_PUBLICATIONS)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("MONITORING_INITIATIVE")).order(60).build(),
            nationalPolicyPreset("oa-7", "National Policy on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                "OA_PUBLICATION_POLICY").order(70).build(),
            nationalMonitoringPreset("oa-8", "National Monitoring on Open Access Publications", OPEN_ACCESS_PUBLICATIONS,
                "MONITORING_INITIATIVE").order(80).build(),
            IndicatorPreset.builder("oa-9", "Distribution of Open Access Types by Different Scholarly Publication Outputs", OPEN_ACCESS_PUBLICATIONS)
                .views(categoryDistributionViews(OA_LICENSE_STATUS_DIMENSION, "Access type"))
                .indicatorCode(PUBLICATIONS_OA_LICENSE_STATUS_COUNT).fullWidth().order(90).build(),

            // ---- Open Data (inventory rows 11-18) -------------------------------------------------------
            IndicatorPreset.builder("od-1", "Open Data in Europe (Open vs Closed)", OPEN_DATA)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(DATASETS_ACCESS_STATUS_SHARE).presetFilters(accessStatus("OA")).order(10).build(),
            IndicatorPreset.builder("od-2", "EU Countries with Policy on Open Data", OPEN_DATA)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("OPEN_DATA_POLICY")).order(20).build(),
            IndicatorPreset.builder("od-3", "Financial investments in Open Data", OPEN_DATA)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("OPEN_DATA")).order(30).build(),
            IndicatorPreset.builder("od-4", "EU Countries with Financial Strategy on Open Data", OPEN_DATA)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("OPEN_DATA_FINANCIAL_STRATEGY")).order(40).build(),
            IndicatorPreset.builder("od-5", "EU Countries with National Open Data Monitoring Initiatives", OPEN_DATA)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("OPEN_DATA_MONITORING_INITIATIVE")).order(50).build(),
            nationalPolicyPreset("od-6", "National Policy on Open Data", OPEN_DATA, "OPEN_DATA_POLICY").order(60).build(),
            nationalMonitoringPreset("od-7", "National Monitoring on Open Data", OPEN_DATA,
                "OPEN_DATA_MONITORING_INITIATIVE").order(70).build(),
            IndicatorPreset.builder("od-8", "Distribution of Open Data Types by Different Document Type", OPEN_DATA)
                .views(categoryDistributionViews(ACCESS_STATUS_DIMENSION, "Access type"))
                .indicatorCode(DATASETS_COUNT).fullWidth().order(80).build(),

            // ---- FAIR Data (inventory rows 19-24) -----------------------------------------------------
            IndicatorPreset.builder("fair-1", "EU Countries with Policy on FAIR Data", FAIR_DATA)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("FAIR_DATA_POLICY")).order(10).build(),
            IndicatorPreset.builder("fair-2", "Financial investments in FAIR Data", FAIR_DATA)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("FAIR_DATA")).order(20).build(),
            IndicatorPreset.builder("fair-3", "EU Countries with Financial Strategy on FAIR Data", FAIR_DATA)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("FAIR_DATA_FINANCIAL_STRATEGY")).order(30).build(),
            IndicatorPreset.builder("fair-4", "EU Countries with National FAIR Data Monitoring Initiatives", FAIR_DATA)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("FAIR_DATA_MONITORING_INITIATIVE")).order(40).build(),
            nationalPolicyPreset("fair-5", "National Policy on FAIR Data", FAIR_DATA, "FAIR_DATA_POLICY").order(50).build(),
            nationalMonitoringPreset("fair-6", "National Monitoring on FAIR Data", FAIR_DATA,
                "FAIR_DATA_MONITORING_INITIATIVE").order(60).build(),

            // ---- Data Management (inventory rows 25-30) ---------------------------------------------
            IndicatorPreset.builder("dm-1", "EU Countries with Policy on Data Management", DATA_MANAGEMENT)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("DATA_MANAGEMENT_POLICY")).order(10).build(),
            IndicatorPreset.builder("dm-2", "Financial investments in Data Management", DATA_MANAGEMENT)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("DATA_MANAGEMENT")).order(20).build(),
            IndicatorPreset.builder("dm-3", "EU Countries with Financial Strategy on Data Management", DATA_MANAGEMENT)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("DATA_MANAGEMENT_FINANCIAL_STRATEGY")).order(30).build(),
            IndicatorPreset.builder("dm-4", "EU Countries with National Data Management Monitoring Initiatives", DATA_MANAGEMENT)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("DATA_MANAGEMENT_MONITORING_INITIATIVE")).order(40).build(),
            nationalPolicyPreset("dm-5", "National Policy on Data Management", DATA_MANAGEMENT, "DATA_MANAGEMENT_POLICY").order(50).build(),
            nationalMonitoringPreset("dm-6", "National Monitoring on Data Management", DATA_MANAGEMENT,
                "DATA_MANAGEMENT_MONITORING_INITIATIVE").order(60).build(),

            // ---- Citizen Science (inventory rows 31-37) -------------------------------------------------
            IndicatorPreset.builder("cs-1", "New Citizen Science projects in EU Countries", CITIZEN_SCIENCE)
                .views(OUTPUT_COUNT_VIEWS_MAP_FULL_WIDTH).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(CITIZEN_SCIENCE_NEW_PROJECTS_COUNT).order(10).build(),
            IndicatorPreset.builder("cs-2", "EU Countries with Policy on Citizen Science", CITIZEN_SCIENCE)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("CITIZEN_SCIENCE_POLICY")).order(20).build(),
            IndicatorPreset.builder("cs-3", "Financial investments in Citizen Science", CITIZEN_SCIENCE)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("CITIZEN_SCIENCE")).order(30).build(),
            IndicatorPreset.builder("cs-4", "EU Countries with Financial Strategy on Citizen Science", CITIZEN_SCIENCE)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("CITIZEN_SCIENCE_FINANCIAL_STRATEGY")).order(40).build(),
            IndicatorPreset.builder("cs-5", "EU Countries with Citizen Science Monitoring Initiatives", CITIZEN_SCIENCE)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("CITIZEN_SCIENCE_MONITORING_INITIATIVE")).order(50).build(),
            nationalPolicyPreset("cs-6", "National Policy on Citizen Science", CITIZEN_SCIENCE, "CITIZEN_SCIENCE_POLICY").order(60).build(),
            nationalMonitoringPreset("cs-7", "National Monitoring on Citizen Science", CITIZEN_SCIENCE,
                "CITIZEN_SCIENCE_MONITORING_INITIATIVE").order(70).build(),

            // ---- Repositories (inventory rows 38-46; covers both Connecting Repositories to EOSC and
            //      Long-term Data Preservation sub-topics) --------------------------------------------
            IndicatorPreset.builder("repo-1", "Repositories offering long-term preservation in EU Countries", REPOSITORIES)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(LONG_TERM_PRESERVATION_COUNT).order(10).build(),
            IndicatorPreset.builder("repo-2", "EU Countries with Policy on Connecting Repositories to EOSC", REPOSITORIES)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("REPOSITORIES_CONNECT_POLICY")).order(20).build(),
            IndicatorPreset.builder("repo-3", "Financial investments in connecting repositories to EOSC", REPOSITORIES)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("REPOSITORIES_CONNECT")).order(30).build(),
            IndicatorPreset.builder("repo-4", "EU Countries with Financial Strategy on Connecting Repositories to EOSC", REPOSITORIES)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY")).order(40).build(),
            IndicatorPreset.builder("repo-5", "EU Countries with Monitoring Initiatives on Connecting Repositories to EOSC", REPOSITORIES)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("REPOSITORIES_CONNECT_MONITORING_INITIATIVE")).order(50).build(),
            nationalPolicyPreset("repo-6", "National Policy on Connecting Repositories to EOSC", REPOSITORIES,
                "REPOSITORIES_CONNECT_POLICY").order(60).build(),
            nationalMonitoringPreset("repo-7", "National Monitoring on Connecting Repositories to EOSC", REPOSITORIES,
                "REPOSITORIES_CONNECT_MONITORING_INITIATIVE").order(70).build(),
            nationalPolicyPreset("repo-8", "National Policy on Long-term Data Preservation", REPOSITORIES,
                "REPOSITORIES_PRESERVATION_POLICY").order(80).build(),
            nationalMonitoringPreset("repo-9", "National Monitoring on Long-term Data Preservation", REPOSITORIES,
                "REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE").order(90).build(),

            // ---- Open Science Training (inventory rows 47-52) ----------------------------------------
            IndicatorPreset.builder("train-1", "EU Countries with Policy on skills/training for Open Science", OPEN_SCIENCE_TRAINING)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("TRAINING_POLICY")).order(10).build(),
            IndicatorPreset.builder("train-2", "Financial investments in skills/training for Open Science", OPEN_SCIENCE_TRAINING)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("TRAINING")).order(20).build(),
            IndicatorPreset.builder("train-3", "EU Countries with Financial Strategy on skills/training in Open Science", OPEN_SCIENCE_TRAINING)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("TRAINING_FINANCIAL_STRATEGY")).order(30).build(),
            IndicatorPreset.builder("train-4", "EU Countries with Monitoring Initiatives on skills/training in Open Science", OPEN_SCIENCE_TRAINING)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("TRAINING_MONITORING_INITIATIVE")).order(40).build(),
            nationalPolicyPreset("train-5", "National Policy on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                "TRAINING_POLICY").order(50).build(),
            nationalMonitoringPreset("train-6", "National Monitoring on Skills/Training in Open Science", OPEN_SCIENCE_TRAINING,
                "TRAINING_MONITORING_INITIATIVE").order(60).build(),

            // ---- Open Software (inventory rows 53-59) -----------------------------------------------
            IndicatorPreset.builder("sw-1", "Open Software sets published in EU Countries", OPEN_SOFTWARE)
                .views(OUTPUT_COUNT_VIEWS_MAP_FULL_WIDTH).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(SOFTWARE_OPEN_SOURCE_SETS_COUNT).order(10).build(),
            IndicatorPreset.builder("sw-2", "EU Countries with Policy on Open Software", OPEN_SOFTWARE)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("SOFTWARE_POLICY")).order(20).build(),
            IndicatorPreset.builder("sw-3", "Financial investments in Open Software", OPEN_SOFTWARE)
                .views(MAGNITUDE_VIEWS).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).presetFilters(investmentArea("SOFTWARE")).order(30).build(),
            IndicatorPreset.builder("sw-4", "EU Countries with Financial Strategy on Open Software", OPEN_SOFTWARE)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("SOFTWARE_FINANCIAL_STRATEGY")).order(40).build(),
            IndicatorPreset.builder("sw-5", "EU Countries with Monitoring Initiatives on Open Software", OPEN_SOFTWARE)
                .views(EU_COVERAGE_VIEWS).indicatorCode(EU_COUNTRY_COVERAGE)
                .presetFilters(initiativeType("SOFTWARE_MONITORING_INITIATIVE")).order(50).build(),
            nationalPolicyPreset("sw-6", "National Policy on Open Source Software", OPEN_SOFTWARE, "SOFTWARE_POLICY").order(60).build(),
            nationalMonitoringPreset("sw-7", "National Monitoring on Open Source Software", OPEN_SOFTWARE,
                "SOFTWARE_MONITORING_INITIATIVE").order(70).build(),

            // ---- Investments in EOSC page (inventory rows 64-65) -----------------------------------
            IndicatorPreset.builder("inv-1", "Total amount of investments in EOSC and Open Science", INVESTMENTS_IN_EOSC)
                .views(totalInvestmentViews()).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).fullWidth().order(10).build(),
            // Same oa.financial_investment indicator as inv-1, broken out per investment area — the
            // definition is groupable by investmentArea (see IndicatorDefinitionCatalog.financialInvestment).
            IndicatorPreset.builder("inv-2", "Investments in EOSC and Open Science (by Open Science area)", INVESTMENTS_IN_EOSC)
                .views(investmentByAreaViews()).allowedSeriesAggregations(AVG_MEDIAN)
                .indicatorCode(FINANCIAL_INVESTMENT).fullWidth().order(20).build(),

            // ---- Open Science Trends (inventory rows 60-63) ----------------------------------------
            // The "… by Areas" rows are eu_country_coverage pinned to every topic's *_POLICY /
            // *_FINANCIAL_STRATEGY member (an IN filter); the initiativeType selector on each view
            // makes the preset-query endpoint group by initiativeType, so one series comes back per area.
            // The SELECTED_COUNTRIES/TIME_RANGE cell offers 3 renderings of the same
            // initiativeType+country query response, switched client-side with no re-query — see
            // IndicatorPreset.groupByFor, which derives groupBy from the cell/selector alone.
            IndicatorPreset.builder("ost-1", "Trends in National Open Science Policies by Areas", OPEN_SCIENCE_TRENDS)
                .views(withTimeRangeSwitcher(areaTrendViews(openScienceAreaSelector()), AREA_TREND_TIME_RANGE_STYLES))
                .indicatorCode(EU_COUNTRY_COVERAGE).presetFilters(initiativeTypes(POLICY_AREA_TYPES)).fullWidth().order(10).build(),
            IndicatorPreset.builder("ost-2", "Trends in Financial Strategy on EOSC and Open Science by Areas", OPEN_SCIENCE_TRENDS)
                .views(withTimeRangeSwitcher(areaTrendViews(openScienceAreaSelector()), AREA_TREND_TIME_RANGE_STYLES))
                .indicatorCode(EU_COUNTRY_COVERAGE).presetFilters(initiativeTypes(FINANCIAL_STRATEGY_AREA_TYPES)).fullWidth().order(20).build(),
            // ost-3/ost-4 back on publications.count / datasets.count grouped by accessStatus (the
            // *_access_status_share indicators can't fan out per access type — accessStatus is a
            // required filter-only binding there). Every view carries an accessStatus selector.
            IndicatorPreset.builder("ost-3", "Trend of Open Access Publications", OPEN_SCIENCE_TRENDS)
                .views(accessTypeTrendViews()).allowedSeriesAggregations(AVG_MEDIAN).indicatorCode(PUBLICATIONS_COUNT).order(30).build(),
            IndicatorPreset.builder("ost-4", "Trend of Open Data", OPEN_SCIENCE_TRENDS)
                .views(accessTypeTrendViews()).allowedSeriesAggregations(AVG_MEDIAN).indicatorCode(DATASETS_COUNT).fullWidth().order(40).build(),

            // ---- National Monitoring (inventory rows 66-69, merged into 2 presets) ---------------
            IndicatorPreset.builder("nm-1", "National Monitoring of Open Science by Areas", NATIONAL_MONITORING)
                .views(withTimeRangeSwitcher(areaTrendViews(openScienceAreaSelector()), AREA_TREND_TIME_RANGE_STYLES_WITHOUT_COVERAGE_TREND))
                .indicatorCode(EU_COUNTRY_COVERAGE).presetFilters(initiativeTypes(MONITORING_AREA_TYPES)).fullWidth().order(10).build(),
            IndicatorPreset.builder("nm-2", "National Monitoring (number of monitored areas per country)", NATIONAL_MONITORING)
                .views(areaCountViews()).indicatorCode(MONITORED_AREAS_COUNT).fullWidth().order(20).build(),

            // ---- Open Science Policies (inventory rows 70-75) ----------------------------------------
            IndicatorPreset.builder("osp-1", "National Open Science Policies by Areas", OPEN_SCIENCE_POLICIES)
                .views(withTimeRangeSwitcher(areaTrendViews(openScienceAreaSelector()), AREA_TREND_TIME_RANGE_STYLES))
                .indicatorCode(EU_COUNTRY_COVERAGE).presetFilters(initiativeTypes(POLICY_AREA_TYPES)).fullWidth().order(10).build(),
            IndicatorPreset.builder("osp-2", "Financial Strategy on EOSC and Open Science by Areas", OPEN_SCIENCE_POLICIES)
                .views(withTimeRangeSwitcher(areaTrendViews(openScienceAreaSelector()), AREA_TREND_TIME_RANGE_STYLES))
                .indicatorCode(EU_COUNTRY_COVERAGE).presetFilters(initiativeTypes(FINANCIAL_STRATEGY_AREA_TYPES)).fullWidth().order(20).build(),
            IndicatorPreset.builder("osp-3", "National Policies (number of policy areas per country)", OPEN_SCIENCE_POLICIES)
                .views(areaCountViews()).indicatorCode(POLICY_AREAS_COUNT).fullWidth().order(30).build(),
            // Pinned to a single initiativeType value, so no selector — grouping by initiativeType
            // here would return one no-op series; groupByFor falls back to grouping by country.
            IndicatorPreset.builder("osp-4", "National Policy on area", OPEN_SCIENCE_POLICIES)
                .views(withTimeRangeSwitcher(areaTrendViews(null), AREA_TREND_TIME_RANGE_STYLES))
                .indicatorCode(COUNTRY_POLICY_STATUS).presetFilters(initiativeType("OA_PUBLICATION_POLICY"))
                .valueLabels(PresetValueLabels.forMissing(NOT_AVAILABLE)).fullWidth().order(40).build(),
            // Registered for catalog completeness — not queryable yet, returns HTTP 501 (see
            // IndicatorDefinitionCatalog.researcherPolicyCoverage() / docs/INDICATOR_TODO.md).
            // researcher_policy_coverage has no initiativeType binding, so areaTrendViews takes no selector.
            IndicatorPreset.builder("osp-5", "Coverage of Researchers by National Open Science Policies in Europe", OPEN_SCIENCE_POLICIES)
                .views(withTimeRangeSwitcher(areaTrendViews(null), AREA_TREND_TIME_RANGE_STYLES))
                .indicatorCode(RESEARCHER_POLICY_COVERAGE).fullWidth().order(50).build()
//            IndicatorPreset.builder("osp-6", "National Open Science Policies by Countries and Areas", OPEN_SCIENCE_POLICIES)
//                .views(areaTrendViews(openScienceAreaSelector()))
//                .indicatorCode(COUNTRY_POLICY_STATUS).presetFilters(initiativeTypes(POLICY_AREA_TYPES))
//                .valueLabels(PresetValueLabels.forMissing(NOT_AVAILABLE)).fullWidth().order(60).build()
        );
    }

    /**
     * "National Policy on &lt;topic&gt;" preset: national-status views over
     * {@code oa.country_policy_status}, pinned to the {@code *_POLICY} {@code initiativeType}
     * member {@code policyType}. Its raw values are display-ready, so only the gap-filled
     * {@code null} is relabeled (to {@value #NOT_AVAILABLE}). Returns a {@link IndicatorPreset.Builder}
     * rather than a finished preset — the caller still chains {@code .order(...).build()}.
     */
    private static IndicatorPreset.Builder nationalPolicyPreset(
            String id, String label, IndicatorGroup group, String policyType) {
        return IndicatorPreset.builder(id, label, group)
            .views(NATIONAL_STATUS_VIEWS)
            .indicatorCode(COUNTRY_POLICY_STATUS)
            .presetFilters(initiativeType(policyType))
            .valueLabels(PresetValueLabels.forMissing(NOT_AVAILABLE));
    }

    /**
     * "National Monitoring on &lt;topic&gt;" preset: national-status views over
     * {@code oa.country_initiative_status}, pinned to the {@code *_MONITORING_INITIATIVE}
     * {@code initiativeType} member {@code monitoringType}. Its raw
     * {@code YES}/{@code NO}/{@code AWAITING_DATA} values (and the gap-filled {@code null}) map to
     * the {@link #MONITORING_STATUS_LABELS} legend labels. Returns a {@link IndicatorPreset.Builder}
     * rather than a finished preset — the caller still chains {@code .order(...).build()}.
     */
    private static IndicatorPreset.Builder nationalMonitoringPreset(
            String id, String label, IndicatorGroup group, String monitoringType) {
        return IndicatorPreset.builder(id, label, group)
            .views(NATIONAL_STATUS_VIEWS)
            .indicatorCode(COUNTRY_INITIATIVE_STATUS)
            .presetFilters(initiativeType(monitoringType))
            .valueLabels(new PresetValueLabels(MONITORING_STATUS_LABELS, NOT_AVAILABLE));
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
