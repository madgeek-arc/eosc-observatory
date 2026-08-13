package eu.openaire.observatory.indicator.handler;

/**
 * Numerator/denominator -> percentage-share, shared by every RATIO indicator's handler: null when
 * the denominator is zero (matching each definition's declared
 * {@code RatioPolicy(ZeroDenominatorHandling.NULL)}), otherwise rounded to one decimal place.
 */
public final class RatioPercentage {

    private RatioPercentage() {
    }

    public static Object of(long numerator, long denominator) {
        return denominator == 0 ? null : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }
}
