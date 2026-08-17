package eu.openaire.observatory.indicator.result;

/**
 * A comparisonMode data point's value. For MEASURE indicators, {@code absolute} is the raw
 * difference and {@code percent} is relative change ({@code null} on a zero baseline). For RATIO
 * indicators (whose raw value is already a percentage), {@code absolute} is always {@code null}
 * and {@code percent} is the percentage-point difference instead (always defined, no division
 * involved).
 */
public record ComparisonValue(
    Number absolute,
    Double percent
) {
}
