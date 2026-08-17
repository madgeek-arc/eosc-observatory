package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.result.ComparisonValue;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ComparisonCalculatorTest {

    private final ComparisonCalculator calculator = new ComparisonCalculator();

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
    void nullModeReturnsResultUnchanged() {
        var result = resultWithPeriods(Map.of("2020", 1000, "2022", 1230));

        var compared = calculator.compare(result, null, new ResolvedTimeRange(2020, 2022, null), IndicatorSemanticType.MEASURE);

        assertThat(compared).isSameAs(result);
    }

    @Test
    void twoPointComputesAbsoluteAndPercentForMeasure() {
        var result = resultWithPeriods(Map.of("2020", 1000, "2022", 1230));
        var range = new ResolvedTimeRange(2020, 2022, null);

        var compared = calculator.compare(result, ComparisonMode.TWO_POINT, range, IndicatorSemanticType.MEASURE);

        assertThat(compared.data()).hasSize(1);
        var point = compared.data().get(0);
        assertThat(point.dimensions())
            .containsEntry(ComparisonCalculator.FROM_PERIOD_DIMENSION, "2020")
            .containsEntry(ComparisonCalculator.TO_PERIOD_DIMENSION, "2022");
        assertThat(point.value()).isEqualTo(new ComparisonValue(230L, 23.0));
    }

    @Test
    void twoPointReturnsNullPercentOnZeroBaselineForMeasure() {
        var result = resultWithPeriods(Map.of("2020", 0, "2022", 500));
        var range = new ResolvedTimeRange(2020, 2022, null);

        var compared = calculator.compare(result, ComparisonMode.TWO_POINT, range, IndicatorSemanticType.MEASURE);

        var value = (ComparisonValue) compared.data().get(0).value();
        assertThat(value.absolute()).isEqualTo(500L);
        assertThat(value.percent()).isNull();
    }

    @Test
    void twoPointComputesPercentagePointDifferenceForRatio() {
        var result = resultWithPeriods(Map.of("2020", 42.0, "2022", 51.0));
        var range = new ResolvedTimeRange(2020, 2022, null);

        var compared = calculator.compare(result, ComparisonMode.TWO_POINT, range, IndicatorSemanticType.RATIO);

        var value = (ComparisonValue) compared.data().get(0).value();
        assertThat(value.absolute()).isNull();
        assertThat(value.percent()).isEqualTo(9.0);
    }

    @Test
    void consecutiveProducesOnePairPerAdjacentYear() {
        var result = resultWithPeriods(Map.of("2020", 100, "2021", 110, "2022", 125, "2023", 125));
        var range = new ResolvedTimeRange(2020, 2023, null);

        var compared = calculator.compare(result, ComparisonMode.CONSECUTIVE, range, IndicatorSemanticType.MEASURE);

        assertThat(compared.data()).hasSize(3);
        assertThat(pairValue(compared, "2020", "2021")).isEqualTo(new ComparisonValue(10L, 10.0));
        assertThat(pairValue(compared, "2021", "2022")).isEqualTo(new ComparisonValue(15L, 13.6));
        assertThat(pairValue(compared, "2022", "2023")).isEqualTo(new ComparisonValue(0L, 0.0));
    }

    @Test
    void diffsEachCountrysSeriesIndependently() {
        List<IndicatorDataPoint> data = List.of(
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2020"), 100),
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2022"), 150),
            new IndicatorDataPoint(Map.of("country", "DE", TimeSeriesNormalizer.PERIOD_DIMENSION, "2020"), 200),
            new IndicatorDataPoint(Map.of("country", "DE", TimeSeriesNormalizer.PERIOD_DIMENSION, "2022"), 180)
        );
        var result = new IndicatorResult(
            new IndicatorMetadata("publications.count", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            data,
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(2020, 2022, null);

        var compared = calculator.compare(result, ComparisonMode.TWO_POINT, range, IndicatorSemanticType.MEASURE);

        assertThat(compared.data()).hasSize(2);
        var gr = compared.data().stream().filter(dp -> "GR".equals(dp.dimensions().get("country"))).findFirst().orElseThrow();
        var de = compared.data().stream().filter(dp -> "DE".equals(dp.dimensions().get("country"))).findFirst().orElseThrow();
        assertThat(gr.value()).isEqualTo(new ComparisonValue(50L, 50.0));
        assertThat(de.value()).isEqualTo(new ComparisonValue(-20L, -10.0));
    }

    @Test
    void missingValuePropagatesAsNullNull() {
        var result = resultWithPeriods(Map.of("2022", 100)); // 2020 absent entirely
        var range = new ResolvedTimeRange(2020, 2022, null);

        var compared = calculator.compare(result, ComparisonMode.TWO_POINT, range, IndicatorSemanticType.MEASURE);

        var value = (ComparisonValue) compared.data().get(0).value();
        assertThat(value.absolute()).isNull();
        assertThat(value.percent()).isNull();
    }

    @Test
    void resultDimensionsSwapPeriodForFromAndToPeriod() {
        var result = resultWithPeriods(Map.of("2020", 100, "2022", 150));
        var range = new ResolvedTimeRange(2020, 2022, null);

        var compared = calculator.compare(result, ComparisonMode.TWO_POINT, range, IndicatorSemanticType.MEASURE);

        assertThat(compared.dimensions())
            .doesNotContain(TimeSeriesNormalizer.PERIOD_DIMENSION)
            .contains(ComparisonCalculator.FROM_PERIOD_DIMENSION, ComparisonCalculator.TO_PERIOD_DIMENSION);
    }

    private static ComparisonValue pairValue(IndicatorResult result, String from, String to) {
        IndicatorDataPoint point = result.data().stream()
            .filter(dp -> from.equals(dp.dimensions().get(ComparisonCalculator.FROM_PERIOD_DIMENSION))
                && to.equals(dp.dimensions().get(ComparisonCalculator.TO_PERIOD_DIMENSION)))
            .findFirst()
            .orElseThrow();
        return (ComparisonValue) point.value();
    }
}
