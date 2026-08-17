package eu.openaire.observatory.indicator.query;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;

public record TimeSeriesRequest(
    Integer from,
    Integer to,
    MissingPeriodHandling missingPeriods // nullable; falls back to TimePolicy default
) {
}
