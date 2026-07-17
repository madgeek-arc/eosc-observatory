package eu.openaire.observatory.indicator.result;

import java.util.Map;

public record IndicatorDataPoint(
    Map<String, Object> dimensions,
    Object value
) {
}
