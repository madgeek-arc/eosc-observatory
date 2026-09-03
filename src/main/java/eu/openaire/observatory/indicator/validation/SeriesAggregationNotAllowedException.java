package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.AggregationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SeriesAggregationNotAllowedException extends IndicatorValidationException {
    public SeriesAggregationNotAllowedException(String presetId, AggregationType aggregation) {
        super("Preset '" + presetId + "' does not allow the series aggregation '" + aggregation
            + "' — see its allowedSeriesAggregations");
    }
}
