package eu.openaire.observatory.indicator.model;

import java.util.Set;

public record AggregationPolicy(
    AggregationType defaultAggregation,
    Set<AggregationType> allowedAggregations,

    // Only meaningful when the owning IndicatorDefinition.kind() == RATIO.
    String numeratorIndicatorCode,
    String denominatorIndicatorCode,
    AggregationType componentAggregation, // how numerator/denominator are each aggregated, e.g. SUM

    NullHandling nullHandling
) {
    public AggregationPolicy {
        allowedAggregations = Set.copyOf(allowedAggregations);
    }
}
