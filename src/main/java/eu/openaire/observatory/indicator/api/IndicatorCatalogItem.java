package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;

import java.util.List;

/**
 * UI-facing catalog projection — deliberately excludes executionBinding and internal policy
 * objects. Exposes every dimension the indicator can be filtered/grouped by (not just country) so
 * a dynamic builder can discover and render filter controls generically, per indicator. {@code
 * format} and {@code renderHint} are two orthogonal rendering axes: format is how to format the
 * value (percentage/number/currency/category/yes-no), renderHint is whether it's a scalar or a
 * per-entity map — kept separate rather than folded into one field, unlike the format string this
 * replaced.
 */
public record IndicatorCatalogItem(
    String id,
    String label,
    String format, // "percentage" | "number" | "currency" | "category" | "yes-no"
    String renderHint, // "SCALAR" | "ENTITY_MAP"
    List<IndicatorDimensionCapability> dimensions,
    boolean supportsTimeRange
) {
    public static IndicatorCatalogItem from(IndicatorDefinition definition, DimensionDefinitionLookup dimensionLookup) {
        List<IndicatorDimensionCapability> dimensions = definition.dimensionBindings().stream()
            .map(binding -> IndicatorDimensionCapability.from(binding, dimensionLookup.getRequired(binding.dimensionCode())))
            .toList();
        return new IndicatorCatalogItem(
            definition.code(), definition.label(), definition.format().wireValue(), definition.renderHint().name(),
            dimensions, definition.timePolicy().supported()
        );
    }
}
