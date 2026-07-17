package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.FilterOperator;

import java.util.List;

public record ResolvedFilter(
    String dimensionCode,
    FilterOperator operator,
    List<Object> values
) {
}
