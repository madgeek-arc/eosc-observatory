package eu.openaire.observatory.indicator.model;

import java.util.Set;

/**
 * A dimension is registered once and referenced by code from any number of indicators — it is
 * never owned or duplicated per indicator.
 */
public record DimensionDefinition(
    String code,
    String label,
    DimensionType type,
    boolean filterable,
    boolean groupable,
    boolean sortable,
    Set<FilterOperator> supportedOperators,
    String parentDimensionCode // nullable; reserved for future hierarchies, unused in v1
) {
    public DimensionDefinition {
        supportedOperators = Set.copyOf(supportedOperators);
    }
}
