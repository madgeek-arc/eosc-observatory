package eu.openaire.observatory.indicator.model;

import java.util.Set;

/**
 * Indicator-specific binding to a shared {@link DimensionDefinition}: which usages (filter/group/
 * sort/time) this indicator allows, and which operators this indicator's execution source actually
 * supports for it — may be narrower than the dimension's own supportedOperators.
 *
 * <p>{@code groupByRequiredForMultipleValues}: a filter on this dimension with several values only
 * makes sense when the dimension is also grouped by, since the execution source returns one point
 * per member rather than combining them.
 */
public record IndicatorDimensionBinding(
    String dimensionCode,
    Set<DimensionUsage> usages,
    Set<FilterOperator> allowedOperators,
    boolean requiredFilter,
    boolean groupByRequiredForMultipleValues
) {
    public IndicatorDimensionBinding(
            String dimensionCode, Set<DimensionUsage> usages, Set<FilterOperator> allowedOperators, boolean requiredFilter) {
        this(dimensionCode, usages, allowedOperators, requiredFilter, false);
    }

    public IndicatorDimensionBinding {
        usages = Set.copyOf(usages);
        allowedOperators = Set.copyOf(allowedOperators);
    }
}
