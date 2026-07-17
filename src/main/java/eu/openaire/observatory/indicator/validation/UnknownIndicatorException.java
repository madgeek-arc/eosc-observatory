package eu.openaire.observatory.indicator.validation;

public class UnknownIndicatorException extends RuntimeException {
    public UnknownIndicatorException(String code) {
        super("Unknown indicator: " + code);
    }
}
