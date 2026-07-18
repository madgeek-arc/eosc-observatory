package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;

import java.util.List;

/**
 * UI-facing catalog projection — deliberately excludes executionBinding and internal policy
 * objects. Exposes every dimension the indicator can be filtered/grouped by (not just country) so
 * a dynamic builder can discover and render filter controls generically, per indicator.
 */
public record IndicatorCatalogItem(
    String id,
    String label,
    String format, // "percentage" | "number" | "map"
    String group,
    List<IndicatorDimensionCapability> dimensions,
    boolean supportsTimeRange
) {
    public static IndicatorCatalogItem from(IndicatorDefinition definition, DimensionDefinitionLookup dimensionLookup) {
        String format = definition.renderHint() == RenderHint.ENTITY_MAP ? "map"
            : definition.unit() == UnitType.PERCENT ? "percentage"
            : "number";
        List<IndicatorDimensionCapability> dimensions = definition.dimensionBindings().stream()
            .map(binding -> IndicatorDimensionCapability.from(binding, dimensionLookup.getRequired(binding.dimensionCode())))
            .toList();
        return new IndicatorCatalogItem(
            definition.code(), definition.label(), format, definition.groupLabel(),
            dimensions, definition.timePolicy().supported()
        );
    }
}
