package eu.openaire.observatory.indicator.result;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.UnitType;

public record IndicatorMetadata(
    String indicatorCode,
    String label,
    IndicatorValueType valueType,
    UnitType unit,
    AggregationType aggregation
) {
}
