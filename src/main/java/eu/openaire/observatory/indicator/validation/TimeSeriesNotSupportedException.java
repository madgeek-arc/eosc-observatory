package eu.openaire.observatory.indicator.validation;

public class TimeSeriesNotSupportedException extends RuntimeException {
    public TimeSeriesNotSupportedException(String code) {
        super("Indicator " + code + " does not support time-series queries");
    }
}
