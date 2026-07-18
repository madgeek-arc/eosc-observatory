package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.TimeSeriesNormalizer;
import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandler;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Synthesizes deterministic (not truly random) mock data, standing in until real handlers exist.
 * Shape of the generated data is driven entirely by the already-validated IndicatorExecutionPlan:
 * every dimension in plan.groupBy() (not just country) produces one row per member of its domain
 * (narrowed by a matching filter if present, else the dimension's full member list), crossed as a
 * cartesian product, crossed again with the requested periods.
 */
@Service
public class MockIndicatorQueryHandler implements IndicatorQueryHandler {

    private static final String EU_AGGREGATE_SCOPE = "EU";
    private static final String CURRENT_PERIOD = "current";

    private final DimensionMemberProviderRegistry dimensionMemberProviderRegistry;

    public MockIndicatorQueryHandler(DimensionMemberProviderRegistry dimensionMemberProviderRegistry) {
        this.dimensionMemberProviderRegistry = dimensionMemberProviderRegistry;
    }

    @Override
    public String handlerKey() {
        return MockIndicatorCatalog.HANDLER_KEY;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        IndicatorDefinition definition = plan.definition();

        List<String> groupByDimensionCodes = plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
        List<String> periods = plan.timeRange() != null
            ? yearsInRange(plan.timeRange().from(), plan.timeRange().to())
            : List.of();

        List<String> dimensionKeys = new ArrayList<>(groupByDimensionCodes);
        if (!periods.isEmpty()) {
            dimensionKeys.add(TimeSeriesNormalizer.PERIOD_DIMENSION);
        }

        List<String> filterSeedParts = plan.filters().stream()
            .sorted(Comparator.comparing(ResolvedFilter::dimensionCode))
            .map(filter -> filter.dimensionCode() + "=" + filter.values())
            .toList();

        List<Map<String, Object>> combinations = groupCombinations(groupByDimensionCodes, plan.filters(), definition.code());

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (Map<String, Object> combination : combinations) {
            String seedScope = combination.isEmpty()
                ? EU_AGGREGATE_SCOPE
                : combination.values().stream().map(String::valueOf).collect(Collectors.joining(","));
            addPoints(data, definition, periods, combination, seedScope, filterSeedParts);
        }

        IndicatorMetadata metadata = new IndicatorMetadata(
            definition.code(), definition.label(), definition.valueType(), definition.unit(), plan.aggregation()
        );
        QueryExecutionMetadata execution = new QueryExecutionMetadata(Instant.now(), false);
        return new IndicatorResult(metadata, dimensionKeys, data, execution);
    }

    /** Cartesian product of each groupBy dimension's resolved value domain; a single empty map when groupBy is empty. */
    private List<Map<String, Object>> groupCombinations(List<String> dimensionCodes, List<ResolvedFilter> filters, String indicatorCode) {
        List<Map<String, Object>> combinations = new ArrayList<>();
        combinations.add(new LinkedHashMap<>());
        for (String dimensionCode : dimensionCodes) {
            List<String> values = resolveGroupValues(dimensionCode, filters, indicatorCode);
            List<Map<String, Object>> expanded = new ArrayList<>();
            for (Map<String, Object> partial : combinations) {
                for (String value : values) {
                    Map<String, Object> extended = new LinkedHashMap<>(partial);
                    extended.put(dimensionCode, value);
                    expanded.add(extended);
                }
            }
            combinations = expanded;
        }
        return combinations;
    }

    /** A matching filter narrows the domain; otherwise the full member list from the dimension's provider. */
    private List<String> resolveGroupValues(String dimensionCode, List<ResolvedFilter> filters, String indicatorCode) {
        List<String> filterValues = filters.stream()
            .filter(filter -> filter.dimensionCode().equals(dimensionCode))
            .flatMap(filter -> filter.values().stream())
            .map(String::valueOf)
            .toList();
        if (!filterValues.isEmpty()) {
            return filterValues;
        }
        return dimensionMemberProviderRegistry.search(dimensionCode, indicatorCode, "", 1000)
            .members().stream()
            .map(DimensionMember::code)
            .toList();
    }

    private void addPoints(List<IndicatorDataPoint> data, IndicatorDefinition definition, List<String> periods,
                            Map<String, Object> baseDimensions, String seedScope, List<String> filterSeedParts) {
        if (periods.isEmpty()) {
            data.add(new IndicatorDataPoint(baseDimensions, mockValue(definition, seedScope, CURRENT_PERIOD, filterSeedParts)));
            return;
        }
        for (String year : periods) {
            Map<String, Object> dimensions = new LinkedHashMap<>(baseDimensions);
            dimensions.put(TimeSeriesNormalizer.PERIOD_DIMENSION, year);
            data.add(new IndicatorDataPoint(dimensions, mockValue(definition, seedScope, year, filterSeedParts)));
        }
    }

    private Object mockValue(IndicatorDefinition definition, String scope, String period, List<String> filterSeedParts) {
        double unitInterval = stableUnitInterval(definition.code(), scope, period, filterSeedParts);
        if (definition.renderHint() == RenderHint.ENTITY_MAP) {
            return unitInterval < 0.5;
        }
        if (definition.unit() == UnitType.PERCENT) {
            return Math.round(unitInterval * 1000) / 10.0; // 0.0 - 100.0, 1 decimal
        }
        return Math.round(unitInterval * 49_500_000 + 500_000); // 500,000 - 50,000,000
    }

    /**
     * Folds every resolved filter (not just country) into the seed, so e.g. different
     * initiativeType values against the same country/year produce visibly different mock values.
     */
    private double stableUnitInterval(String indicatorCode, String scope, String period, List<String> filterSeedParts) {
        List<String> parts = new ArrayList<>(List.of(indicatorCode, scope, period));
        parts.addAll(filterSeedParts);
        return new Random(Arrays.hashCode(parts.toArray())).nextDouble();
    }

    private List<String> yearsInRange(LocalDate from, LocalDate to) {
        List<String> years = new ArrayList<>();
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            years.add(String.valueOf(year));
        }
        return years;
    }
}
