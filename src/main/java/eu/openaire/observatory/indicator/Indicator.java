package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.ChartType;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import eu.openaire.observatory.indicator.validation.PresetFilterConflictException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A preset indicator: an {@link IndicatorDefinition} pinned to specific preset filter values. */
public record Indicator(
    String id,
    String label,
    IndicatorGroup group,
    ChartType chartType,
    List<AggregationType> allowedSeriesAggregations, // meaningful seriesAggregations values for this preset — empty if none
    String indicatorCode, // must resolve in IndicatorDefinitionCatalog
    Map<String, List<String>> presetFilters // dimensionCode -> values; empty when none needed
) {
    /**
     * Merges this preset's indicatorCode/presetFilters into {@code request}; {@code
     * request.indicatorCode()} is ignored. Every other field passes through unchanged;
     * {@code request.filters()} is appended after the preset filters.
     *
     * @throws PresetFilterConflictException if request.filters() names an already-preset dimension.
     */
    public IndicatorQuery toIndicatorQuery(IndicatorQuery request) {
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
            indicatorCode, null, filters, List.of(), timeSeries, null, List.of(), null, resolvedSeriesAggregations));
    }
}
