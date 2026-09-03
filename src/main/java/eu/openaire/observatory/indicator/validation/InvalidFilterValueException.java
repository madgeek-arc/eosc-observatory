package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFilterValueException extends IndicatorValidationException {
    public InvalidFilterValueException(String dimensionCode, Object value) {
        super("Value '" + value + "' is not a valid member of dimension " + dimensionCode);
    }
}
