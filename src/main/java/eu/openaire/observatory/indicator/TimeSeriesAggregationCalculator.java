package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes one or more whole-series summary statistics (e.g. AVG, MEDIAN) across an entire time
 * series — a different axis from {@code IndicatorQuery.aggregation}, which collapses per-dimension
 * values (e.g. per country) within a single period. Groups {@code result.data()} by every dimension
 * except "period" — the same convention {@link TimeSeriesNormalizer}/{@link ComparisonCalculator}
 * already use — so a multi-country series gets one summary per country, not one number blended across
 * countries and years together.
 */
@Service
public class TimeSeriesAggregationCalculator {

    public static final String AGGREGATION_DIMENSION = "aggregation";

    public IndicatorResult summarize(IndicatorResult result, List<AggregationType> types) {
        if (types.isEmpty()) {
            return result;
        }

        Map<Map<String, Object>, List<IndicatorDataPoint>> byGroup = new LinkedHashMap<>();
        for (IndicatorDataPoint point : result.data()) {
            Map<String, Object> groupKey = new LinkedHashMap<>(point.dimensions());
            groupKey.remove(TimeSeriesNormalizer.PERIOD_DIMENSION);
            byGroup.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(point);
        }

        List<IndicatorDataPoint> summary = new ArrayList<>();
        for (var group : byGroup.entrySet()) {
            List<Number> values = group.getValue().stream()
                .sorted(Comparator.comparingInt(dp ->
                    Integer.parseInt(String.valueOf(dp.dimensions().get(TimeSeriesNormalizer.PERIOD_DIMENSION)))))
                .map(IndicatorDataPoint::value)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .toList();

            for (AggregationType type : types) {
                Map<String, Object> dimensions = new LinkedHashMap<>(group.getKey());
                dimensions.put(AGGREGATION_DIMENSION, type.name());
                summary.add(new IndicatorDataPoint(dimensions, aggregate(type, values)));
            }
        }

        return new IndicatorResult(result.metadata(), result.dimensions(), result.data(), result.execution(), summary);
    }

    /**
     * Small, deliberate duplicate of SurveyStatsToolIndicatorQueryHandler's private
     * aggregateNumbers/median — generalized to a heterogeneous List<Number> (this axis operates on
     * whatever Object values already sit in result.data(), from any handler, not a single handler's
     * typed per-country map), so it isn't the same generic shape and doesn't share code cleanly.
     * Preserves Long results for all-Long input (a count series' SUM stays 186, not 186.0).
     */
    private Object aggregate(AggregationType type, List<Number> values) {
        if (values.isEmpty()) {
            return null;
        }
        boolean allLongs = allLongs(values);
        // Each branch is boxed through an Object-returning helper before the ternary sees it —
        // a bare `allLongs ? longSum : doubleSum` would trigger Java's binary numeric promotion
        // (JLS 15.25) and silently widen the long branch to double even when allLongs is true.
        return switch (type) {
            case SUM -> allLongs ? sumLongs(values) : sumDoubles(values);
            case AVG -> values.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
            case MEDIAN -> median(values);
            case MIN -> allLongs ? minLong(values) : minDouble(values);
            case MAX -> allLongs ? maxLong(values) : maxDouble(values);
            case FIRST -> values.get(0);
            case LAST -> values.get(values.size() - 1);
            default -> throw new IllegalStateException("Unsupported seriesAggregation: " + type);
        };
    }

    private boolean allLongs(List<Number> values) {
        return values.stream().allMatch(v -> v instanceof Long);
    }

    private Object sumLongs(List<Number> values) {
        return values.stream().mapToLong(Number::longValue).sum();
    }

    private Object sumDoubles(List<Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).sum();
    }

    private Object minLong(List<Number> values) {
        return values.stream().mapToLong(Number::longValue).min().orElseThrow();
    }

    private Object minDouble(List<Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).min().orElseThrow();
    }

    private Object maxLong(List<Number> values) {
        return values.stream().mapToLong(Number::longValue).max().orElseThrow();
    }

    private Object maxDouble(List<Number> values) {
        return values.stream().mapToDouble(Number::doubleValue).max().orElseThrow();
    }

    /** Even-count lists average the two middle values (always a Double) — odd-count lists return the natural middle element untouched. */
    private Object median(List<Number> values) {
        List<Number> sorted = values.stream().sorted(Comparator.comparingDouble(Number::doubleValue)).toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1).doubleValue() + sorted.get(middle).doubleValue()) / 2.0;
    }
}
