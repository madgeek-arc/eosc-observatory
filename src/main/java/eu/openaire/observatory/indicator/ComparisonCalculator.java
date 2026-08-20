package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.result.ComparisonValue;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a raw per-period IndicatorResult (already gap-filled by TimeSeriesNormalizer) into
 * period-over-period deltas, grouping by every dimension except "period" the same way
 * TimeSeriesNormalizer does, so a multi-country result is diffed per country independently.
 */
@Service
public class ComparisonCalculator {

    public static final String FROM_PERIOD_DIMENSION = "fromPeriod";
    public static final String TO_PERIOD_DIMENSION = "toPeriod";

    public IndicatorResult compare(
            IndicatorResult result, ComparisonMode mode, ResolvedTimeRange timeRange, IndicatorSemanticType semanticType) {
        if (mode == null) {
            return result;
        }

        Map<Map<String, Object>, Map<String, IndicatorDataPoint>> byGroupThenPeriod = new LinkedHashMap<>();
        for (IndicatorDataPoint point : result.data()) {
            Map<String, Object> groupKey = new LinkedHashMap<>(point.dimensions());
            Object period = groupKey.remove(TimeSeriesNormalizer.PERIOD_DIMENSION);
            byGroupThenPeriod
                .computeIfAbsent(groupKey, k -> new LinkedHashMap<>())
                .put(String.valueOf(period), point);
        }

        List<IndicatorDataPoint> compared = new ArrayList<>();
        for (var group : byGroupThenPeriod.entrySet()) {
            Map<String, Object> groupKey = group.getKey();
            Map<String, IndicatorDataPoint> byPeriod = group.getValue();
            for (int[] pair : pairsFor(mode, timeRange)) {
                IndicatorDataPoint base = byPeriod.get(String.valueOf(pair[0]));
                IndicatorDataPoint target = byPeriod.get(String.valueOf(pair[1]));
                Map<String, Object> dimensions = new LinkedHashMap<>(groupKey);
                dimensions.put(FROM_PERIOD_DIMENSION, String.valueOf(pair[0]));
                dimensions.put(TO_PERIOD_DIMENSION, String.valueOf(pair[1]));
                compared.add(new IndicatorDataPoint(dimensions, delta(base, target, semanticType)));
            }
        }

        List<String> dimensions = new ArrayList<>(result.dimensions());
        dimensions.remove(TimeSeriesNormalizer.PERIOD_DIMENSION);
        dimensions.add(FROM_PERIOD_DIMENSION);
        dimensions.add(TO_PERIOD_DIMENSION);

        return new IndicatorResult(result.metadata(), dimensions, compared, result.execution(), result.summary());
    }

    private List<int[]> pairsFor(ComparisonMode mode, ResolvedTimeRange timeRange) {
        List<int[]> pairs = new ArrayList<>();
        if (mode == ComparisonMode.TWO_POINT) {
            pairs.add(new int[] {timeRange.from(), timeRange.to()});
        } else {
            for (int year = timeRange.from(); year < timeRange.to(); year++) {
                pairs.add(new int[] {year, year + 1});
            }
        }
        return pairs;
    }

    /** MEASURE -> {absolute diff, relative % change (null on zero baseline)}. RATIO -> {null, point difference}. */
    private ComparisonValue delta(IndicatorDataPoint base, IndicatorDataPoint target, IndicatorSemanticType semanticType) {
        Object baseValue = base != null ? base.value() : null;
        Object targetValue = target != null ? target.value() : null;
        if (!(baseValue instanceof Number baseNumber) || !(targetValue instanceof Number targetNumber)) {
            return new ComparisonValue(null, null);
        }

        if (semanticType == IndicatorSemanticType.RATIO) {
            return new ComparisonValue(null, round(targetNumber.doubleValue() - baseNumber.doubleValue()));
        }

        long baseCount = baseNumber.longValue();
        long targetCount = targetNumber.longValue();
        Double percent = baseCount == 0 ? null : round((targetCount - baseCount) / (double) baseCount * 100.0);
        return new ComparisonValue(targetCount - baseCount, percent);
    }

    /** Same one-decimal rounding convention as RatioPercentage, for UI consistency. */
    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
