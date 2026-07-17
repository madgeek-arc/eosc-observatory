package eu.openaire.observatory.indicator.model;

import java.util.Set;

public record AggregationPolicy(
    AggregationType defaultAggregation,
    Set<AggregationType> allowedAggregations,
    NullHandling nullHandling
) {
    public AggregationPolicy {
        allowedAggregations = Set.copyOf(allowedAggregations);
    }
}
