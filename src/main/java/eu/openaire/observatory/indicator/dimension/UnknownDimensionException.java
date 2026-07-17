package eu.openaire.observatory.indicator.dimension;

public class UnknownDimensionException extends RuntimeException {
    public UnknownDimensionException(String dimensionCode) {
        super("No member provider registered for dimension " + dimensionCode);
    }
}
