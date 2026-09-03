package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidComparisonRangeException extends IndicatorValidationException {
    public InvalidComparisonRangeException(String code, int from, int to) {
        super("Indicator " + code + ": comparisonMode requires from != to (got from=" + from + ", to=" + to + ")");
    }
}
