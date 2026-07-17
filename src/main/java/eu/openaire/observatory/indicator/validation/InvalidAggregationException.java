package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.AggregationType;

public class InvalidAggregationException extends RuntimeException {
    public InvalidAggregationException(String code, AggregationType aggregation) {
        super("Aggregation " + aggregation + " not allowed for indicator " + code);
    }
}
