package eu.openaire.observatory.indicator.query;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.TimeGrain;

import java.time.LocalDate;

public record TimeSeriesRequest(
    TimeGrain grain,
    LocalDate from,
    LocalDate to,
    MissingPeriodHandling missingPeriods // nullable; falls back to TimePolicy default
) {
}
