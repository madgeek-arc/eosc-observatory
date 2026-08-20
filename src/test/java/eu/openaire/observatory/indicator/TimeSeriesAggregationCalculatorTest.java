package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSeriesAggregationCalculatorTest {

    private final TimeSeriesAggregationCalculator calculator = new TimeSeriesAggregationCalculator();

    private static IndicatorResult resultWithPeriods(Map<String, Object> periodToValue) {
        List<IndicatorDataPoint> data = periodToValue.entrySet().stream()
            .map(e -> new IndicatorDataPoint(Map.of(TimeSeriesNormalizer.PERIOD_DIMENSION, e.getKey()), e.getValue()))
            .toList();
        return new IndicatorResult(
            new IndicatorMetadata("publications.count", "label", null, null, null),
            List.of(TimeSeriesNormalizer.PERIOD_DIMENSION),
            data,
            new QueryExecutionMetadata(Instant.now(), false)
        );
    }

    @Test
    void emptyTypesReturnsResultUnchanged() {
        var result = resultWithPeriods(Map.of("2020", 10, "2021", 20, "2022", 30));

        var summarized = calculator.summarize(result, List.of());

        assertThat(summarized).isSameAs(result);
    }

    @Test
    void computesAvgAndMedianAcrossTheWholeSeries() {
        var result = resultWithPeriods(Map.of("2020", 10L, "2021", 20L, "2022", 30L));

        var summarized = calculator.summarize(result, List.of(AggregationType.AVG, AggregationType.MEDIAN));

        assertThat(summarized.data()).isSameAs(result.data());
        assertThat(summarized.summary()).hasSize(2);
        assertThat(aggregationValue(summarized, "AVG")).isEqualTo(20.0);
        assertThat(aggregationValue(summarized, "MEDIAN")).isEqualTo(20L);
    }

    @Test
    void summarizesEachGroupIndependentlyWhenGroupedByCountry() {
        List<IndicatorDataPoint> data = List.of(
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2020"), 100L),
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2021"), 200L),
            new IndicatorDataPoint(Map.of("country", "DE", TimeSeriesNormalizer.PERIOD_DIMENSION, "2020"), 10L),
            new IndicatorDataPoint(Map.of("country", "DE", TimeSeriesNormalizer.PERIOD_DIMENSION, "2021"), 30L)
        );
        var result = new IndicatorResult(
            new IndicatorMetadata("publications.count", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            data,
            new QueryExecutionMetadata(Instant.now(), false)
        );

        var summarized = calculator.summarize(result, List.of(AggregationType.AVG));

        assertThat(summarized.summary()).hasSize(2);
        assertThat(summarized.summary()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get("country")).isEqualTo("GR");
            assertThat(dp.value()).isEqualTo(150.0);
        });
        assertThat(summarized.summary()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get("country")).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(20.0);
        });
    }

    @Test
    void excludesNullValuesFromTheComputation() {
        Map<String, Object> periodToValue = new java.util.LinkedHashMap<>();
        periodToValue.put("2020", 10L);
        periodToValue.put("2021", null);
        periodToValue.put("2022", 30L);
        var result = resultWithPeriods(periodToValue);

        var summarized = calculator.summarize(result, List.of(AggregationType.AVG));

        assertThat(aggregationValue(summarized, "AVG")).isEqualTo(20.0);
    }

    @Test
    void firstAndLastRespectChronologicalOrderRegardlessOfInputOrder() {
        List<IndicatorDataPoint> data = List.of(
            new IndicatorDataPoint(Map.of(TimeSeriesNormalizer.PERIOD_DIMENSION, "2022"), 30L),
            new IndicatorDataPoint(Map.of(TimeSeriesNormalizer.PERIOD_DIMENSION, "2020"), 10L),
            new IndicatorDataPoint(Map.of(TimeSeriesNormalizer.PERIOD_DIMENSION, "2021"), 20L)
        );
        var result = new IndicatorResult(
            new IndicatorMetadata("publications.count", "label", null, null, null),
            List.of(TimeSeriesNormalizer.PERIOD_DIMENSION),
            data,
            new QueryExecutionMetadata(Instant.now(), false)
        );

        var summarized = calculator.summarize(result, List.of(AggregationType.FIRST, AggregationType.LAST));

        assertThat(aggregationValue(summarized, "FIRST")).isEqualTo(10L);
        assertThat(aggregationValue(summarized, "LAST")).isEqualTo(30L);
    }

    @Test
    void sumPreservesLongForAnAllIntegerSeriesButDoubleForADecimalOne() {
        var longResult = resultWithPeriods(Map.of("2020", 10L, "2021", 20L));
        var doubleResult = resultWithPeriods(Map.of("2020", 10.5, "2021", 20.0));

        assertThat(aggregationValue(calculator.summarize(longResult, List.of(AggregationType.SUM)), "SUM")).isEqualTo(30L);
        assertThat(aggregationValue(calculator.summarize(doubleResult, List.of(AggregationType.SUM)), "SUM")).isEqualTo(30.5);
    }

    private static Object aggregationValue(IndicatorResult result, String aggregation) {
        return result.summary().stream()
            .filter(dp -> aggregation.equals(dp.dimensions().get(TimeSeriesAggregationCalculator.AGGREGATION_DIMENSION)))
            .findFirst()
            .orElseThrow()
            .value();
    }
}
