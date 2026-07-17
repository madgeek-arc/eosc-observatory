package eu.openaire.observatory.indicator.model;

import java.util.Set;
import java.util.UUID;

/**
 * Storage-agnostic on purpose: whatever persistence is chosen later (registry/JSON resource type
 * or JPA) maps into this record; the record itself has no framework dependency.
 */
public record IndicatorDefinition(
    UUID id,
    String code, // stable external identifier, e.g. "publications.count"
    String label,
    String description,
    IndicatorValueType valueType,
    IndicatorKind kind,
    UnitType unit,
    AggregationPolicy aggregationPolicy,
    Set<IndicatorDimensionRef> dimensions, // references DimensionDefinition by code
    TimePolicy timePolicy,
    IndicatorAccessLevel accessLevel,
    IndicatorStatus status,
    int version
) {
    public IndicatorDefinition {
        dimensions = Set.copyOf(dimensions);
    }
}
