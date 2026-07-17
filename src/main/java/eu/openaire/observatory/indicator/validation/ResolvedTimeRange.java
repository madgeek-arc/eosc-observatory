package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.TimeGrain;

import java.time.LocalDate;

public record ResolvedTimeRange(
    TimeGrain grain,
    LocalDate from,
    LocalDate to,
    MissingPeriodHandling missingPeriods
) {
}
