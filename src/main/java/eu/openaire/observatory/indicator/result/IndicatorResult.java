package eu.openaire.observatory.indicator.result;

import java.util.List;

/** Visualization-neutral: no chart-library shapes leak out of the query service. */
public record IndicatorResult(
    IndicatorMetadata metadata,
    List<String> dimensions,
    List<IndicatorDataPoint> data,
    QueryExecutionMetadata execution
) {
}
