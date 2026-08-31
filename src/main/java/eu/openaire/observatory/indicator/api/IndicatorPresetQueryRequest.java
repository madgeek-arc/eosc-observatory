package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.AggregationType;

import java.util.List;

/**
 * Body of {@code POST /indicators/presets/{id}/query}. A preset indicator is select-and-nudge:
 * only the country selection, the year range, and the whole-series summary statistics vary at
 * use time — everything else (indicatorCode, filters, grouping) is fixed by the preset. These are
 * the same four scope inputs {@code PUT /dashboard} carries, validated by the same rules
 * ({@link eu.openaire.observatory.indicator.validation.PresetScopeValidation}).
 *
 * <ul>
 *   <li>{@code countries} — null/empty means no country filter (EU-wide aggregate).
 *   <li>{@code yearTo} — required.
 *   <li>{@code yearFrom} — required only when the preset's indicator supports a time series;
 *       unused for survey-backed presets, where {@code yearTo} alone picks the survey year. Must
 *       be {@code <= yearTo} when given.
 *   <li>{@code seriesAggregations} — null/empty means none; every value must be in the preset's
 *       {@code allowedSeriesAggregations}.
 * </ul>
 */
public record IndicatorPresetQueryRequest(
    List<String> countries,
    Integer yearFrom,
    Integer yearTo,
    List<AggregationType> seriesAggregations
) {
}
