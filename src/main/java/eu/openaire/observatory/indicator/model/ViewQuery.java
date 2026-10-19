package eu.openaire.observatory.indicator.model;

import java.util.List;
import java.util.Map;

/**
 * The query a single {@link PresetView} runs in place of its preset's own: a different indicator,
 * its own pinned filters and an explicit {@code groupBy}. Used when one cell of a preset needs data
 * that its preset's indicator does not provide.
 *
 * <p>{@code presetFilters} maps a dimension code to its pinned values, like
 * {@link eu.openaire.observatory.indicator.IndicatorPreset#presetFilters()}. {@code groupBy} is applied
 * verbatim.
 */
public record ViewQuery(
    String indicatorCode,
    Map<String, List<String>> presetFilters,
    List<String> groupBy
) {
    public ViewQuery {
        if (indicatorCode == null || indicatorCode.isBlank()) {
            throw new IllegalArgumentException("ViewQuery.indicatorCode is required");
        }
        presetFilters = presetFilters == null ? Map.of() : Map.copyOf(presetFilters);
        groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
    }
}
