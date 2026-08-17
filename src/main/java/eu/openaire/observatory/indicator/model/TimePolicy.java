package eu.openaire.observatory.indicator.model;

public record TimePolicy(
    boolean supported,
    MissingPeriodHandling missingPeriodHandling
) {
}
