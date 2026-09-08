package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a survey-stats-tool indicator is queried with neither a {@code timeSeries} range nor
 * a {@code surveyYear} filter — there is no "latest survey" to default to, so the query has no year
 * to resolve. A 400 (rather than the {@link IllegalStateException} the handler would otherwise
 * throw) because the caller can fix it by supplying either one.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MissingTimeScopeException extends IndicatorValidationException {
    public MissingTimeScopeException(String indicatorCode) {
        super("Indicator " + indicatorCode + " requires either a timeSeries range or a surveyYear filter");
    }
}
