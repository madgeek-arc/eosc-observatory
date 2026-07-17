package eu.openaire.observatory.indicator.validation;

public class IndicatorAccessDeniedException extends RuntimeException {
    public IndicatorAccessDeniedException(String code) {
        super("Access denied for indicator: " + code);
    }
}
