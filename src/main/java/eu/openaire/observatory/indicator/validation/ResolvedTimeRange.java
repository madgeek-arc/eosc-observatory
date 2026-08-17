package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;

public record ResolvedTimeRange(
    int from,
    int to,
    MissingPeriodHandling missingPeriods
) {
}
