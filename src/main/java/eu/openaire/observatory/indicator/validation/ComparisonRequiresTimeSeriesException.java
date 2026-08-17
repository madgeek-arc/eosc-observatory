package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ComparisonRequiresTimeSeriesException extends RuntimeException {
    public ComparisonRequiresTimeSeriesException(String code) {
        super("Indicator " + code + ": comparisonMode requires a timeSeries range");
    }
}
