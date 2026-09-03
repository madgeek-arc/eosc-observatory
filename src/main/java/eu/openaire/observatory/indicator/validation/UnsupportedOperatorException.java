package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.FilterOperator;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedOperatorException extends IndicatorValidationException {
    public UnsupportedOperatorException(String dimensionCode, FilterOperator operator) {
        super("Operator " + operator + " not supported for dimension " + dimensionCode);
    }
}
