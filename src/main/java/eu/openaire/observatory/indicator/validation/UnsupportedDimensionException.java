package eu.openaire.observatory.indicator.validation;

public class UnsupportedDimensionException extends RuntimeException {
    public UnsupportedDimensionException(String indicatorCode, String dimensionCode) {
        super("Dimension " + dimensionCode + " not supported by indicator " + indicatorCode);
    }
}
