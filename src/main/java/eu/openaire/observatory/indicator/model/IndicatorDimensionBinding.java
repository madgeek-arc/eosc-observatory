package eu.openaire.observatory.indicator.model;

import java.util.Set;

/**
 * Indicator-specific binding to a shared {@link DimensionDefinition}: which usages (filter/group/
 * sort/time) this indicator allows, and which operators this indicator's execution source actually
 * supports for it — may be narrower than the dimension's own supportedOperators.
 */
public record IndicatorDimensionBinding(
    String dimensionCode,
    Set<DimensionUsage> usages,
    Set<FilterOperator> allowedOperators,
    boolean requiredFilter
) {
    public IndicatorDimensionBinding {
        usages = Set.copyOf(usages);
        allowedOperators = Set.copyOf(allowedOperators);
    }
}
