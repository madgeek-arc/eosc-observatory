package eu.openaire.observatory.indicator.validation;

public class MissingRequiredFilterException extends RuntimeException {
    public MissingRequiredFilterException(String indicatorCode, String dimensionCode) {
        super("Indicator " + indicatorCode + " requires a filter on dimension " + dimensionCode);
    }
}
