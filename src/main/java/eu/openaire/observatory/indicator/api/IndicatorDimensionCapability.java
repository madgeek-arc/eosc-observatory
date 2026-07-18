package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.model.DimensionType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;

import java.util.Set;

/** UI-facing description of one dimension an indicator can be filtered/grouped by, driving a dynamic builder's filter controls. */
public record IndicatorDimensionCapability(
    String dimensionCode,
    String label,
    DimensionType type,
    boolean filterable,
    boolean groupable,
    boolean requiredFilter,
    Set<FilterOperator> allowedOperators
) {
    public static IndicatorDimensionCapability from(IndicatorDimensionBinding binding, DimensionDefinition dimension) {
        return new IndicatorDimensionCapability(
            binding.dimensionCode(),
            dimension.label(),
            dimension.type(),
            binding.usages().contains(DimensionUsage.FILTER),
            binding.usages().contains(DimensionUsage.GROUP),
            binding.requiredFilter(),
            binding.allowedOperators()
        );
    }
}
