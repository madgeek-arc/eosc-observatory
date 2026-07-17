package eu.openaire.observatory.indicator.model;

import java.util.Set;

public record TimePolicy(
    boolean supported,
    String timeDimensionCode, // references a DimensionDefinition, e.g. "publicationYear"
    Set<TimeGrain> supportedGrains,
    TimeGrain defaultGrain,
    TemporalBehavior behavior,
    MissingPeriodHandling missingPeriodHandling
) {
    public TimePolicy {
        supportedGrains = Set.copyOf(supportedGrains);
    }
}
