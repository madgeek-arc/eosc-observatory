package eu.openaire.observatory.indicator.validation;

public class UnknownDimensionException extends RuntimeException {
    public UnknownDimensionException(String code) {
        super("No dimension registered for code: " + code);
    }
}
