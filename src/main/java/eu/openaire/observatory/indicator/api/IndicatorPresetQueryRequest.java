package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.AggregationType;

import java.util.List;

/**
 * Body of {@code POST /indicators/presets/{id}/query}. A preset indicator is select-and-nudge:
 * only the country selection, the year range, and the whole-series summary statistics vary at
 * use time — everything else (indicatorCode, filters, grouping) is fixed by the preset.
 *
 * <p>{@code yearFrom} is unused for survey-backed presets, where {@code yearTo} alone picks the
 * survey year. {@code seriesAggregations} is checked against the preset's
 * {@code allowedSeriesAggregations}.
 */
public record IndicatorPresetQueryRequest(
    List<String> countries,
    Integer yearFrom,
    Integer yearTo,
    List<AggregationType> seriesAggregations
) {
}
