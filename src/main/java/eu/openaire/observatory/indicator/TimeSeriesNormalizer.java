package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills gaps in a time-series result according to its MissingPeriodHandling policy. Groups data
 * points by every dimension except "period" (e.g. "country") and gap-fills each group's period
 * series independently, so a multi-country result doesn't have its countries collapsed together.
 *
 * <p>A group only gets gap-filled if the handler returned at least one row for it — a group with
 * zero rows at all (e.g. one selected country with no data in any requested period) stays absent
 * from the output entirely, rather than being null-filled, matching how a handler already drops
 * (rather than nulls) an individual missing period/country pairing within an otherwise-present
 * group. The one exception is a plain aggregate result with no grouping dimension at all: there,
 * zero rows still gets null/zero-filled across the whole requested range, since there's no group
 * identity to have been "dropped".
 */
@Service
public class TimeSeriesNormalizer {

    public static final String PERIOD_DIMENSION = "period";

    public IndicatorResult normalize(IndicatorResult result, ResolvedTimeRange timeRange) {
        if (timeRange == null || timeRange.missingPeriods() == MissingPeriodHandling.OMIT) {
            return result;
        }

        Map<Map<String, Object>, Map<String, IndicatorDataPoint>> byGroupThenPeriod = new LinkedHashMap<>();
        for (IndicatorDataPoint point : result.data()) {
            Map<String, Object> groupKey = new LinkedHashMap<>(point.dimensions());
            Object period = groupKey.remove(PERIOD_DIMENSION);
            byGroupThenPeriod
                .computeIfAbsent(groupKey, k -> new LinkedHashMap<>())
                .put(String.valueOf(period), point);
        }
        // Only fabricate a single ungrouped placeholder group when the result has no grouping
        // dimension at all (a plain aggregate query genuinely returned zero rows) — this lets a
        // NULL/ZERO_FILL policy still produce one point per period. When the result declares a
        // non-period dimension (e.g. "country") but a handler returned zero rows for it — a
        // requested group (a filtered-but-dataless country) is dropped, not null-filled, matching
        // how a *partially* missing group is already handled below — inserting the Map.of()
        // placeholder here would instead fabricate a row with that dimension silently missing,
        // contradicting the result's own declared `dimensions()`. Leave `filled` empty instead.
        boolean hasGroupingDimension = result.dimensions().stream().anyMatch(d -> !d.equals(PERIOD_DIMENSION));
        if (byGroupThenPeriod.isEmpty() && !hasGroupingDimension) {
            byGroupThenPeriod.put(Map.of(), Map.of());
        }

        List<String> periods = generatePeriods(timeRange.from(), timeRange.to());
        List<IndicatorDataPoint> filled = new ArrayList<>();
        for (var group : byGroupThenPeriod.entrySet()) {
            Map<String, Object> groupKey = group.getKey();
            Map<String, IndicatorDataPoint> byPeriod = group.getValue();
            for (String period : periods) {
                IndicatorDataPoint existing = byPeriod.get(period);
                if (existing != null) {
                    filled.add(existing);
                    continue;
                }
                Object value = timeRange.missingPeriods() == MissingPeriodHandling.ZERO_FILL ? 0 : null;
                Map<String, Object> dimensions = new LinkedHashMap<>(groupKey);
                dimensions.put(PERIOD_DIMENSION, period);
                filled.add(new IndicatorDataPoint(dimensions, value));
            }
        }

        return new IndicatorResult(result.metadata(), result.dimensions(), filled, result.execution());
    }

    private List<String> generatePeriods(int from, int to) {
        List<String> periods = new ArrayList<>();
        for (int year = from; year <= to; year++) {
            periods.add(String.valueOf(year));
        }
        return periods;
    }
}
