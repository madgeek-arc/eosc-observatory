package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.TimeSeriesNormalizer;
import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProvider;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandler;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Synthesizes deterministic (not truly random) mock data for the 8 legacy indicators, standing in
 * until real handlers exist. Shape of the generated data is driven entirely by the already-
 * validated IndicatorExecutionPlan, so it transparently supports all four builder combinations:
 * single/multiple countries x single/multiple years.
 */
@Service
public class MockIndicatorQueryHandler implements IndicatorQueryHandler {

    private static final String EU_AGGREGATE_SCOPE = "EU";
    private static final String CURRENT_PERIOD = "current";

    private final DimensionMemberProvider dimensionMemberProvider;

    public MockIndicatorQueryHandler(DimensionMemberProvider dimensionMemberProvider) {
        this.dimensionMemberProvider = dimensionMemberProvider;
    }

    @Override
    public String handlerKey() {
        return MockIndicatorCatalog.HANDLER_KEY;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        IndicatorDefinition definition = plan.definition();
        boolean hasCountryDimension = definition.dimensions().stream()
            .anyMatch(binding -> binding.dimensionCode().equals(MockIndicatorCatalog.COUNTRY_DIMENSION));
        boolean groupByCountry = hasCountryDimension && plan.groupBy().stream()
            .anyMatch(dim -> dim.dimensionCode().equals(MockIndicatorCatalog.COUNTRY_DIMENSION));
        List<String> filteredCountries = hasCountryDimension ? countryFilterValues(plan.filters()) : List.of();

        List<String> periods = plan.timeRange() != null
            ? yearsInRange(plan.timeRange().from(), plan.timeRange().to())
            : List.of();

        List<String> dimensionKeys = new ArrayList<>();
        if (groupByCountry) {
            dimensionKeys.add(MockIndicatorCatalog.COUNTRY_DIMENSION);
        }
        if (!periods.isEmpty()) {
            dimensionKeys.add(TimeSeriesNormalizer.PERIOD_DIMENSION);
        }

        List<IndicatorDataPoint> data = new ArrayList<>();
        if (groupByCountry) {
            List<String> countries = !filteredCountries.isEmpty() ? filteredCountries : allCountryCodes();
            for (String country : countries) {
                addPoints(data, definition, periods, Map.of(MockIndicatorCatalog.COUNTRY_DIMENSION, country), country);
            }
        } else {
            String scope = hasCountryDimension && !filteredCountries.isEmpty()
                ? String.join(",", filteredCountries)
                : EU_AGGREGATE_SCOPE;
            addPoints(data, definition, periods, Map.of(), scope);
        }

        IndicatorMetadata metadata = new IndicatorMetadata(
            definition.code(), definition.label(), definition.valueType(), definition.unit(), plan.aggregation()
        );
        QueryExecutionMetadata execution = new QueryExecutionMetadata(Instant.now(), false);
        return new IndicatorResult(metadata, dimensionKeys, data, execution);
    }

    private void addPoints(List<IndicatorDataPoint> data, IndicatorDefinition definition,
                            List<String> periods, Map<String, Object> baseDimensions, String seedScope) {
        if (periods.isEmpty()) {
            data.add(new IndicatorDataPoint(baseDimensions, mockValue(definition, seedScope, CURRENT_PERIOD)));
            return;
        }
        for (String year : periods) {
            Map<String, Object> dimensions = new LinkedHashMap<>(baseDimensions);
            dimensions.put(TimeSeriesNormalizer.PERIOD_DIMENSION, year);
            data.add(new IndicatorDataPoint(dimensions, mockValue(definition, seedScope, year)));
        }
    }

    private Object mockValue(IndicatorDefinition definition, String scope, String period) {
        double unitInterval = stableUnitInterval(definition.code(), scope, period);
        if (definition.renderHint() == RenderHint.ENTITY_MAP) {
            return unitInterval < 0.5;
        }
        if (definition.unit() == UnitType.PERCENT) {
            return Math.round(unitInterval * 1000) / 10.0; // 0.0 - 100.0, 1 decimal
        }
        return Math.round(unitInterval * 49_500_000 + 500_000); // 500,000 - 50,000,000
    }

    private double stableUnitInterval(String... parts) {
        return new Random(Arrays.hashCode(parts)).nextDouble();
    }

    private List<String> countryFilterValues(List<ResolvedFilter> filters) {
        return filters.stream()
            .filter(filter -> filter.dimensionCode().equals(MockIndicatorCatalog.COUNTRY_DIMENSION))
            .flatMap(filter -> filter.values().stream())
            .map(String::valueOf)
            .toList();
    }

    private List<String> allCountryCodes() {
        return dimensionMemberProvider.search(MockIndicatorCatalog.COUNTRY_DIMENSION, null, "", 1000)
            .members().stream()
            .map(DimensionMember::code)
            .toList();
    }

    private List<String> yearsInRange(LocalDate from, LocalDate to) {
        List<String> years = new ArrayList<>();
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            years.add(String.valueOf(year));
        }
        return years;
    }
}
