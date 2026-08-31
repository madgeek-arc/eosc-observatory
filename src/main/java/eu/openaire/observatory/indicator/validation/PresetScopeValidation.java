package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.IndicatorPreset;
import eu.openaire.observatory.indicator.model.AggregationType;

import java.util.List;

/**
 * The rules a preset indicator's use-time scope ({@code countries} / {@code yearFrom} /
 * {@code yearTo} / {@code seriesAggregations}) must satisfy. Shared by
 * {@code POST /indicators/presets/{id}/query} and the {@code PUT /dashboard} save path so both
 * accept and reject the same inputs.
 */
public final class PresetScopeValidation {

    private PresetScopeValidation() {
    }

    /**
     * @param trendCapable whether the preset's backing indicator supports a time series
     *                     ({@code timePolicy().supported()})
     * @throws SeriesAggregationNotAllowedException a requested series aggregation is not in the
     *                                              preset's {@code allowedSeriesAggregations}
     * @throws InvalidPresetScopeException          {@code yearTo} is null, {@code yearFrom} is null
     *                                              for a trend-capable indicator, or
     *                                              {@code yearFrom > yearTo}
     */
    public static void validate(IndicatorPreset preset, boolean trendCapable,
            Integer yearFrom, Integer yearTo, List<AggregationType> seriesAggregations) {
        List<AggregationType> requested = seriesAggregations == null ? List.of() : seriesAggregations;
        for (AggregationType aggregation : requested) {
            if (!preset.allowedSeriesAggregations().contains(aggregation)) {
                throw new SeriesAggregationNotAllowedException(preset.id(), aggregation);
            }
        }
        if (yearTo == null) {
            throw new InvalidPresetScopeException("yearTo must not be null");
        }
        if (trendCapable && yearFrom == null) {
            throw new InvalidPresetScopeException(
                "yearFrom must not be null: this indicator supports a time series");
        }
        if (yearFrom != null && yearFrom > yearTo) {
            throw new InvalidPresetScopeException("yearFrom must be <= yearTo");
        }
    }
}
