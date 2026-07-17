package eu.openaire.observatory.indicator.query;

import eu.openaire.observatory.indicator.model.FilterOperator;

import java.util.List;

public record IndicatorFilter(
    String dimension,
    FilterOperator operator,
    List<Object> values
) {
}
