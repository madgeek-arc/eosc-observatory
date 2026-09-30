package eu.openaire.observatory.indicator.result;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * One value of a result. {@code total} is what {@code value} is out of, for values that have a
 * natural whole (e.g. the number of areas counted); null otherwise.
 */
public record IndicatorDataPoint(
    Map<String, Object> dimensions,
    Object value,
    @JsonInclude(JsonInclude.Include.NON_NULL) Object total
) {
    public IndicatorDataPoint(Map<String, Object> dimensions, Object value) {
        this(dimensions, value, null);
    }
}
