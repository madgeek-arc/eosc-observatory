package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ComparisonNotSupportedException extends IndicatorValidationException {
    public ComparisonNotSupportedException(String code) {
        super("Indicator " + code + " does not support comparisonMode queries");
    }
}
