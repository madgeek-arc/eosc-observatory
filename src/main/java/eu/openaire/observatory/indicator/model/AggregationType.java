package eu.openaire.observatory.indicator.model;

/**
 * Single-series aggregations only. Ratio is not a member here: it is triggered by
 * {@link IndicatorKind#RATIO} and driven by {@link AggregationPolicy#componentAggregation()},
 * which aggregates the numerator and denominator series before dividing.
 */
public enum AggregationType {
    SUM,
    COUNT,
    COUNT_DISTINCT,
    AVG,
    MIN,
    MAX,
    FIRST,
    LAST,
    NONE
}
