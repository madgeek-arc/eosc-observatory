package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.FilterOperator;

public class UnsupportedOperatorException extends RuntimeException {
    public UnsupportedOperatorException(String dimensionCode, FilterOperator operator) {
        super("Operator " + operator + " not supported for dimension " + dimensionCode);
    }
}
