package eu.openaire.observatory.indicator.result;

import java.util.List;

/** Visualization-neutral: no chart-library shapes leak out of the query service. */
public record IndicatorResult(
    IndicatorMetadata metadata,
    List<String> dimensions,
    List<IndicatorDataPoint> data,
    QueryExecutionMetadata execution,
    // Whole-series summary statistics (e.g. AVG/MEDIAN across every period in the queried range) —
    // see TimeSeriesAggregationCalculator. Empty unless the query set seriesAggregations; never
    // replaces `data`.
    List<IndicatorDataPoint> summary
) {
    public IndicatorResult {
        summary = summary != null ? summary : List.of();
    }

    public IndicatorResult(IndicatorMetadata metadata, List<String> dimensions, List<IndicatorDataPoint> data,
                            QueryExecutionMetadata execution) {
        this(metadata, dimensions, data, execution, List.of());
    }
}
