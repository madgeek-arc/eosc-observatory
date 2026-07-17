package eu.openaire.observatory.indicator.result;

import java.time.Instant;

public record QueryExecutionMetadata(
    Instant generatedAt,
    boolean partial
) {
}
