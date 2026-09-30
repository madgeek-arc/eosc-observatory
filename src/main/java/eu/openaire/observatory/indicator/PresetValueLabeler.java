package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rewrites the raw values in a preset-query result to the display labels the preset declares
 * ({@link IndicatorPreset#labels()}), so the UI can use them straight as chart legend labels. Runs
 * after the whole {@code IndicatorQueryService.execute} pipeline — that is where
 * {@code TimeSeriesNormalizer} adds the gap-filled {@code null}s this also relabels — and only on
 * the {@code POST /indicators/presets/{id}/query} path; the generic {@code POST /indicators/query}
 * contract keeps the raw values.
 */
@Component
public class PresetValueLabeler {

    public IndicatorResult relabel(IndicatorPreset preset, IndicatorResult result) {
        PresetValueLabels labels = preset.labels();
        if (labels.isEmpty()) {
            return result;
        }
        return new IndicatorResult(
            result.metadata(),
            result.dimensions(),
            relabel(labels, result.data()),
            result.execution(),
            relabel(labels, result.summary()),
            result.exclusions());
    }

    private List<IndicatorDataPoint> relabel(PresetValueLabels labels, List<IndicatorDataPoint> points) {
        return points.stream().map(point -> relabelPoint(labels, point)).toList();
    }

    private IndicatorDataPoint relabelPoint(PresetValueLabels labels, IndicatorDataPoint point) {
        Object value = point.value();
        Object relabeled;
        if (value == null) {
            relabeled = labels.missingValue(); // may itself be null — then the point is unchanged
        } else if (value instanceof String raw) {
            relabeled = labels.values().getOrDefault(raw, raw);
        } else {
            relabeled = value; // numbers and the like are never legend categories
        }
        return relabeled == value ? point : new IndicatorDataPoint(point.dimensions(), relabeled, point.total());
    }
}
