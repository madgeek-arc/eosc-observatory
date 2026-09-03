package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.AggregationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAggregationException extends IndicatorValidationException {
    public InvalidAggregationException(String code, AggregationType aggregation) {
        super("Aggregation " + aggregation + " not allowed for indicator " + code);
    }
}
