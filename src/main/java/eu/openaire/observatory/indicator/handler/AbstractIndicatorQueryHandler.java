package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorExclusion;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared filter/dimensionKey/result-building helpers for handlers that read an
 * {@link IndicatorExecutionPlan} and produce a flat {@link IndicatorResult} — every real
 * {@link IndicatorQueryHandler} in this codebase does both.
 */
public abstract class AbstractIndicatorQueryHandler implements IndicatorQueryHandler {

    protected static final String PERIOD_DIMENSION = "period";

    protected static ResolvedFilter requiredFilter(IndicatorExecutionPlan plan, String dimensionCode) {
        return plan.filters().stream()
            .filter(filter -> filter.dimensionCode().equals(dimensionCode))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Expected a required '" + dimensionCode + "' filter to already have been validated"));
    }

    protected static String requiredFilterValue(IndicatorExecutionPlan plan, String dimensionCode) {
        return String.valueOf(requiredFilter(plan, dimensionCode).values().get(0));
    }

    /** Every value of every filter on the given dimension — empty if none, e.g. for an optional country filter. */
    protected static List<String> filterValues(IndicatorExecutionPlan plan, String dimensionCode) {
        return plan.filters().stream()
            .filter(filter -> filter.dimensionCode().equals(dimensionCode))
            .flatMap(filter -> filter.values().stream())
            .map(String::valueOf)
            .toList();
    }

    /** groupByCodes, plus PERIOD_DIMENSION appended when the plan requested a time series. */
    protected static List<String> dimensionKeysFor(List<String> groupByCodes, IndicatorExecutionPlan plan) {
        List<String> dimensionKeys = new ArrayList<>(groupByCodes);
        if (plan.timeRange() != null) {
            dimensionKeys.add(PERIOD_DIMENSION);
        }
        return dimensionKeys;
    }

    protected static IndicatorResult buildResult(IndicatorExecutionPlan plan, List<String> dimensionKeys, List<IndicatorDataPoint> data) {
        return buildResult(plan, dimensionKeys, data, List.of());
    }

    protected static IndicatorResult buildResult(IndicatorExecutionPlan plan, List<String> dimensionKeys,
                                                 List<IndicatorDataPoint> data, List<IndicatorExclusion> exclusions) {
        IndicatorMetadata metadata = new IndicatorMetadata(
            plan.definition().code(), plan.definition().label(), plan.definition().valueType(),
            plan.definition().unit(), plan.aggregation()
        );
        return new IndicatorResult(metadata, dimensionKeys, data, new QueryExecutionMetadata(Instant.now(), false),
            List.of(), exclusions);
    }
}
