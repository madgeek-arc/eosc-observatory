package eu.openaire.observatory.indicator.query;

import eu.openaire.observatory.indicator.model.AggregationType;

import java.util.List;

/** Single-indicator-per-query for the first slice; batching multiple indicators is an open question. */
public record IndicatorQuery(
    String indicatorCode,
    AggregationType aggregation, // null/ignored when the indicator's kind is RATIO
    List<IndicatorFilter> filters,
    List<String> groupBy,
    TimeSeriesRequest timeSeries, // null if not a time-series request
    ComparisonMode comparisonMode, // null unless requesting period-over-period deltas; requires timeSeries
    List<SortRequest> sort,
    Integer limit
) {
    public IndicatorQuery {
        filters = filters != null ? filters : List.of();
        groupBy = groupBy != null ? groupBy : List.of();
        sort = sort != null ? sort : List.of();
    }
}
