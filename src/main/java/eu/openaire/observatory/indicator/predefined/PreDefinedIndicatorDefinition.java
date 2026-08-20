package eu.openaire.observatory.indicator.predefined;

import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One concrete dashboard widget (e.g. "National Policy on Open Data") backed by a real catalog
 * indicator plus the preset filter values that disambiguate it — unlike IndicatorCatalog's generic,
 * parameterized indicators (one indicator can back many widgets, e.g. oa.country_initiative_status
 * serves ~9 different "National Monitoring on X" widgets via its initiativeType filter), each of
 * these is a fixed, named instance meant to be iterated and rendered directly by an Explore-style
 * page — see PreDefinedIndicatorCatalog.
 */
public record PreDefinedIndicatorDefinition(
    String id,
    String label,
    String group,
    PreDefinedIndicatorKind kind,
    PreDefinedIndicatorFormat format,
    boolean hasAverage,
    String indicatorCode, // must resolve in IndicatorCatalog — validated at startup, see PreDefinedIndicatorCatalog
    Map<String, List<String>> presetFilters // dimensionCode -> values; empty when the indicator needs no disambiguating filter
) {
    /**
     * Merges this widget's own indicatorCode/presetFilters into a caller-supplied request, so it can
     * be run through the same IndicatorQueryService/IndicatorQueryValidator pipeline
     * /indicator-queries already uses. {@code request.indicatorCode()} is ignored — this widget's own
     * code always wins. Every other field (aggregation, groupBy, timeSeries, comparisonMode, sort,
     * limit, seriesAggregations) passes through unchanged; {@code request.filters()} is appended after
     * the preset filters.
     *
     * @throws PresetFilterConflictException if request.filters() names a dimension already preset —
     *         a widget's identity is its preset filter value (e.g. "oa-1" is accessStatus=OA), so
     *         silently unioning or overriding it would quietly change what the widget means.
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
}
