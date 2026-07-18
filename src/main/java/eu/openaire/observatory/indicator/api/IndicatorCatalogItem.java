package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.UnitType;

/** UI-facing catalog projection — deliberately excludes executionBinding and internal policy objects. */
public record IndicatorCatalogItem(
    String id,
    String label,
    String format, // "percentage" | "number" | "map"
    String group,
    boolean supportsCountry,
    boolean supportsTimeRange
) {
    public static IndicatorCatalogItem from(IndicatorDefinition definition) {
        String format = definition.renderHint() == RenderHint.ENTITY_MAP ? "map"
            : definition.unit() == UnitType.PERCENT ? "percentage"
            : "number";
        boolean supportsCountry = definition.dimensions().stream()
            .anyMatch(binding -> binding.dimensionCode().equals("country"));
        return new IndicatorCatalogItem(
            definition.code(), definition.label(), format, definition.groupCode(),
            supportsCountry, definition.timePolicy().supported()
        );
    }
}
