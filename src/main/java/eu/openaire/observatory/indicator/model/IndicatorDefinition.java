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
    String groupLabel, // nullable; category label for a UI picker, e.g. "Publications"
    IndicatorValueType valueType,
    IndicatorSemanticType semanticType,
    UnitType unit,
    RenderHint renderHint,
    AggregationPolicy aggregationPolicy,
    RatioPolicy ratioPolicy, // nullable; meaningful only when semanticType == RATIO
    Set<IndicatorDimensionBinding> dimensionBindings, // this indicator's bindings to DimensionDefinitions, by code
    TimePolicy timePolicy,
    IndicatorExecutionBinding executionBinding,
    IndicatorAccessLevel accessLevel,
    IndicatorStatus status,
    int version
) {
    public IndicatorDefinition {
        dimensionBindings = Set.copyOf(dimensionBindings);
    }
}
