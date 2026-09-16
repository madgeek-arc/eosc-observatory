package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
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
 * <p>A group only gets gap-filled if the handler returned at least one row for it, or if it's one
 * of the expected groups re-seeded below — matching how a handler already drops (rather than
 * nulls) an individual missing period/country pairing within an otherwise-present group.
 *
 * <p>When the result declares exactly one grouping dimension (e.g. "country") and the handler
 * returned zero rows for it at all, the expected groups are re-seeded from either the query's own
 * explicit filter on that dimension, or — absent a filter — every member the dimension's
 * registered {@link eu.openaire.observatory.indicator.dimension.DimensionMemberProvider} knows
 * about, so a fully-dataless range still comes back null/zero-filled instead of an empty array.
 * Two or more simultaneous grouping dimensions are left alone in this case (stays empty) —
 * enumerating a full cross-product of every dimension's known members risks producing hundreds of
 * synthetic rows with no real precedent for "expected". A plain aggregate result with no grouping
 * dimension at all is unaffected: zero rows there already gets null/zero-filled across the whole
 * requested range, since there's no group identity involved.
 */
@Service
public class TimeSeriesNormalizer {

    public static final String PERIOD_DIMENSION = "period";

    private final DimensionMemberProviderRegistry memberProviders;

    public TimeSeriesNormalizer(DimensionMemberProviderRegistry memberProviders) {
        this.memberProviders = memberProviders;
    }

    public IndicatorResult normalize(IndicatorResult result, ResolvedTimeRange timeRange, List<ResolvedFilter> filters) {
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
        if (byGroupThenPeriod.isEmpty()) {
            List<String> groupingDimensions = result.dimensions().stream()
                .filter(d -> !d.equals(PERIOD_DIMENSION))
                .toList();
            if (groupingDimensions.isEmpty()) {
                byGroupThenPeriod.put(Map.of(), Map.of());
            } else if (groupingDimensions.size() == 1) {
                String dimensionCode = groupingDimensions.get(0);
                for (Object member : expectedMembers(dimensionCode, result.metadata().indicatorCode(), filters)) {
                    byGroupThenPeriod.put(Map.of(dimensionCode, member), Map.of());
                }
            }
            // 2+ simultaneous grouping dimensions: leave empty, see class javadoc.
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

    /**
     * The dimension values that should appear even with zero data: an explicit filter on that
     * dimension if the query has one (mirrors how a handler already narrows to requested values
     * elsewhere), otherwise every member the dimension's provider knows about. Empty if neither
     * exists — there's no reasonable set to fabricate.
     */
    private List<Object> expectedMembers(String dimensionCode, String indicatorCode, List<ResolvedFilter> filters) {
        for (ResolvedFilter filter : filters) {
            if (filter.dimensionCode().equals(dimensionCode) && !filter.values().isEmpty()) {
                return filter.values();
            }
        }
        return memberProviders.find(dimensionCode)
            .map(provider -> provider.search(indicatorCode, "", Integer.MAX_VALUE).members().stream()
                .<Object>map(DimensionMember::code)
                .toList())
            .orElse(List.of());
    }

    private List<String> generatePeriods(int from, int to) {
        List<String> periods = new ArrayList<>();
        for (int year = from; year <= to; year++) {
            periods.add(String.valueOf(year));
        }
        return periods;
    }
}
