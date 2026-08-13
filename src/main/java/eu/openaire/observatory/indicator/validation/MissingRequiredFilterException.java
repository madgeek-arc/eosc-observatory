package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingRequiredFilterException extends RuntimeException {
    public MissingRequiredFilterException(String indicatorCode, String dimensionCode) {
        super("Indicator " + indicatorCode + " requires a filter on dimension " + dimensionCode);
    }
}
