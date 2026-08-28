package eu.openaire.observatory.indicator.model;

import java.util.List;

/**
 * A breakdown dropdown layered on a {@link PresetView}: the UI offers the actor a choice of members
 * of one dimension, and when they pick one it appends {@code dimension} to the query's
 * {@code groupBy} (see {@link eu.openaire.observatory.indicator.query.IndicatorQuery#groupBy()}).
 *
 * <p>{@code dimension} must be a code the backing {@link IndicatorDefinition} binds with
 * {@link DimensionUsage#GROUP} — the catalog checks this at construction. This is purely
 * presentation metadata: nothing here is persisted on a saved dashboard, and the backend never
 * applies the grouping itself.
 *
 * <p>{@code members} is an optional curated subset (in display order); when empty the UI offers
 * every member, which it can fetch from
 * {@code GET /indicators/{indicatorCode}/dimensions/{dimension}/members}.
 */
public record ViewSelector(
    String dimension,
    String label, // nullable; the UI falls back to the dimension's own label
    List<String> members // empty = offer every member
) {
    public ViewSelector {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("ViewSelector.dimension is required");
        }
        members = members == null ? List.of() : List.copyOf(members);
    }
}
