package eu.openaire.observatory.indicator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.CountryScope;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorFormat;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.model.RenderStyle;
import eu.openaire.observatory.indicator.model.TimeScope;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import eu.openaire.observatory.indicator.validation.PresetFilterConflictException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One dashboard-ready preset indicator: a generic {@link IndicatorDefinition} with filter values
 * pinned ({@code presetFilters}) and presentation attached ({@code label}, {@code group}, {@code
 * views}, {@code format}). Held by {@link IndicatorPresetCatalog} and serialized straight to
 * the {@code GET /indicators/presets} wire — there is no separate DTO.
 *
 * <p>{@code views} lists the renderings that make sense for this preset — normally one per cell
 * of the {@code countryScope × timeScope} 2×2. The UI picks the entry matching the actor's
 * current filter state; the backend never decides or persists a rendering.
 *
 * <p>{@code format} is copied from the linked definition by the catalog (see
 * {@link #resolvedAgainst(IndicatorDefinition)}); authored entries leave it {@code null}.
 *
 * <p>{@code labels} ({@link PresetValueLabels}) lets a preset name the display strings its query
 * results carry — a raw per-country status value like {@code "YES"} maps to
 * {@code "Has national monitoring"}, a gap-filled {@code null} year to {@code "N/A"} — so the UI
 * can use the values straight as chart legend labels. It serializes flat as
 * {@code valueLabels}/{@code missingValueLabel} (both omitted when empty) and is applied to the
 * {@code POST /indicators/presets/{id}/query} response by {@link PresetValueLabeler}; the generic
 * {@code POST /indicators/query} endpoint is untouched. {@link PresetValueLabels#NONE} for every
 * preset whose raw values are already display-ready.
 */
public record IndicatorPreset(
    String id,
    String label,
    IndicatorGroup group,
    List<PresetView> views, // renderings that make sense for this preset; UI picks by the actor's filter state
    IndicatorFormat format, // resolved from the linked IndicatorDefinition by IndicatorPresetCatalog
    List<AggregationType> allowedSeriesAggregations, // meaningful seriesAggregations values for this preset — empty if none
    String indicatorCode, // must resolve in IndicatorDefinitionCatalog
    Map<String, List<String>> presetFilters, // dimensionCode -> values; empty when none needed
    @JsonIgnore PresetValueLabels labels, // legend labels for this preset's raw values; serialized flat, see valueLabels()/missingValueLabel()
    @JsonIgnore int order // this preset indicator's rank within {@code group}; drives catalog ordering, not exposed on the wire
) {
    public IndicatorPreset {
        labels = labels != null ? labels : PresetValueLabels.NONE;
    }

    /** Raw data-point value -&gt; display label; empty when the raw values are already display-ready. */
    @JsonProperty("valueLabels")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public Map<String, String> valueLabels() {
        return labels.values();
    }

    /** Display label for a null (gap-filled) data-point value; {@code null} to leave nulls as-is. */
    @JsonProperty("missingValueLabel")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String missingValueLabel() {
        return labels.missingValue();
    }

    /** A copy with {@code format} filled from {@code definition}. Called once, at catalog construction. */
    public IndicatorPreset resolvedAgainst(IndicatorDefinition definition) {
        return new IndicatorPreset(
            id, label, group, views, definition.format(),
            allowedSeriesAggregations, indicatorCode, presetFilters, labels, order);
    }

    /** A copy carrying {@code labels} — used by the catalog to attach a preset's legend labels. */
    public IndicatorPreset withValueLabels(PresetValueLabels labels) {
        return new IndicatorPreset(id, label, group, views, format, allowedSeriesAggregations,
            indicatorCode, presetFilters, labels, order);
    }

    /**
     * Merges this preset's indicatorCode/presetFilters into {@code request}; {@code
     * request.indicatorCode()} is ignored. Every other field passes through unchanged;
     * {@code request.filters()} is appended after the preset filters.
     *
     * @throws PresetFilterConflictException if request.filters() names an already-preset dimension.
     */
    IndicatorQuery toIndicatorQuery(IndicatorQuery request) {
        List<IndicatorFilter> merged = new ArrayList<>();
        presetFilters.forEach((dimensionCode, values) -> {
            FilterOperator operator = values.size() == 1 ? FilterOperator.EQ : FilterOperator.IN;
            merged.add(new IndicatorFilter(dimensionCode, operator, List.copyOf(values)));
        });
        for (IndicatorFilter filter : request.filters()) {
            if (presetFilters.containsKey(filter.dimension())) {
                throw new PresetFilterConflictException(id, filter.dimension());
            }
            merged.add(filter);
        }

        return new IndicatorQuery(
            indicatorCode, request.aggregation(), merged, request.groupBy(), request.timeSeries(),
            request.comparisonMode(), request.sort(), request.limit(), request.seriesAggregations()
        );
    }

    /**
     * Builds the query for this preset from {@code countries}/{@code yearFrom}/{@code yearTo}/
     * {@code seriesAggregations}. {@code countries}, if given, becomes a country filter. When
     * {@code indicator.timePolicy().supported()}, the year range becomes a {@link TimeSeriesRequest};
     * otherwise {@code yearTo} becomes a single {@code surveyYear} EQ filter and {@code yearFrom} is
     * unused.
     *
     * <p>{@code groupBy} is derived from {@code views}: the backend computes the same
     * {@code (countryScope, timeScope)} cell the UI would render, then —
     * <ul>
     *   <li>if that view carries a {@link PresetView#selector()} (the "distribution" presets), it
     *       groups by the selector's dimension so one series comes back per member; the UI shows the
     *       member the actor picked. {@code country} is added only for the selected-countries cells,
     *       and {@code classification} only for the stacked-column cell (and only when the backing
     *       indicator binds it for grouping — the other cells aggregate it away client-side);
     *   <li>otherwise it groups by country exactly when that view's
     *       {@link eu.openaire.observatory.indicator.model.RenderStyle#perCountryBreakdown()}.
     * </ul>
     */
    public IndicatorQuery toIndicatorQuery(
            IndicatorDefinition indicator, List<String> countries, Integer yearFrom, Integer yearTo,
            List<AggregationType> seriesAggregations) {
        List<IndicatorFilter> filters = new ArrayList<>();
        if (countries != null && !countries.isEmpty()) {
            filters.add(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION,
                countries.size() == 1 ? FilterOperator.EQ : FilterOperator.IN, List.copyOf(countries)));
        }
        TimeSeriesRequest timeSeries = null;
        if (indicator.timePolicy().supported()) {
            timeSeries = new TimeSeriesRequest(yearFrom, yearTo, null);
        } else if (yearTo != null) {
            filters.add(new IndicatorFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of(yearTo)));
        }
        List<AggregationType> resolvedSeriesAggregations = seriesAggregations == null ? List.of() : seriesAggregations;
        return toIndicatorQuery(new IndicatorQuery(
            indicatorCode, null, filters, groupByFor(indicator, countries, yearFrom, yearTo), timeSeries, null,
            List.of(), null, resolvedSeriesAggregations));
    }

    private List<String> groupByFor(IndicatorDefinition indicator, List<String> countries, Integer yearFrom, Integer yearTo) {
        CountryScope countryScope = (countries == null || countries.isEmpty())
            ? CountryScope.ALL_COUNTRIES : CountryScope.SELECTED_COUNTRIES;
        TimeScope timeScope = (yearFrom == null || yearFrom.equals(yearTo))
            ? TimeScope.SINGLE_YEAR : TimeScope.TIME_RANGE;
        PresetView matched = views.stream()
            .filter(view -> (view.countryScope() == null || view.countryScope() == countryScope)
                && (view.timeScope() == null || view.timeScope() == timeScope))
            .findFirst()
            .orElse(null);
        if (matched == null) {
            return List.of();
        }
        if (matched.selector() != null) {
            List<String> groupBy = new ArrayList<>();
            groupBy.add(matched.selector().dimension());
            if (matched.renderStyle() == RenderStyle.STACKED_COLUMN
                    && bindsForGrouping(indicator, IndicatorCodes.CLASSIFICATION_DIMENSION)) {
                groupBy.add(IndicatorCodes.CLASSIFICATION_DIMENSION);
            }
            if (countryScope == CountryScope.SELECTED_COUNTRIES) {
                groupBy.add(IndicatorCodes.COUNTRY_DIMENSION);
            }
            return List.copyOf(groupBy);
        }
        return matched.renderStyle().perCountryBreakdown()
            ? List.of(IndicatorCodes.COUNTRY_DIMENSION) : List.of();
    }

    private static boolean bindsForGrouping(IndicatorDefinition indicator, String dimensionCode) {
        return indicator.dimensionBindings().stream()
            .anyMatch(binding -> binding.dimensionCode().equals(dimensionCode)
                && binding.usages().contains(DimensionUsage.GROUP));
    }
}
