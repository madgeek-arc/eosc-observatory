package eu.openaire.observatory.indicator.validation;

/** A malformed filter rejected before member discovery or query execution. */
public class InvalidFilterShapeException extends IndicatorValidationException {
    public InvalidFilterShapeException(String filter, String reason) {
        super("Invalid filter " + filter + ": " + reason);
    }
}
