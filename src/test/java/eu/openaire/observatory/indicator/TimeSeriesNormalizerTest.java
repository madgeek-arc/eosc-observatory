package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSeriesNormalizerTest {

    private final TimeSeriesNormalizer normalizer = new TimeSeriesNormalizer();

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
    void zeroFillsMissingYearsWithZero() {
        var result = resultWithPeriods(Map.of("2020", 10, "2022", 20));
        var range = new ResolvedTimeRange(
            TimeGrain.YEAR, LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), MissingPeriodHandling.ZERO_FILL
        );

        var normalized = normalizer.normalize(result, range);

        assertThat(normalized.data()).hasSize(3);
        assertThat(valueFor(normalized, "2020")).isEqualTo(10);
        assertThat(valueFor(normalized, "2021")).isEqualTo(0);
        assertThat(valueFor(normalized, "2022")).isEqualTo(20);
    }

    @Test
    void nullFillsMissingYears() {
        var result = resultWithPeriods(Map.of("2020", 10));
        var range = new ResolvedTimeRange(
            TimeGrain.YEAR, LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1), MissingPeriodHandling.NULL_FILL
        );

        var normalized = normalizer.normalize(result, range);

        assertThat(normalized.data()).hasSize(2);
        assertThat(valueFor(normalized, "2020")).isEqualTo(10);
        assertThat(valueFor(normalized, "2021")).isNull();
    }

    @Test
    void zeroFillsEachCountrysSeriesIndependently() {
        List<IndicatorDataPoint> data = List.of(
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2020"), 10),
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2022"), 30),
            new IndicatorDataPoint(Map.of("country", "DE", TimeSeriesNormalizer.PERIOD_DIMENSION, "2021"), 20)
        );
        var result = new IndicatorResult(
            new IndicatorMetadata("publications.count", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            data,
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(
            TimeGrain.YEAR, LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), MissingPeriodHandling.ZERO_FILL
        );

        var normalized = normalizer.normalize(result, range);

        assertThat(normalized.data()).hasSize(6);
        assertThat(valueForCountry(normalized, "GR", "2020")).isEqualTo(10);
        assertThat(valueForCountry(normalized, "GR", "2021")).isEqualTo(0);
        assertThat(valueForCountry(normalized, "GR", "2022")).isEqualTo(30);
        assertThat(valueForCountry(normalized, "DE", "2020")).isEqualTo(0);
        assertThat(valueForCountry(normalized, "DE", "2021")).isEqualTo(20);
        assertThat(valueForCountry(normalized, "DE", "2022")).isEqualTo(0);
    }

    @Test
    void omitLeavesGapsUntouched() {
        var result = resultWithPeriods(Map.of("2020", 10));
        var range = new ResolvedTimeRange(
            TimeGrain.YEAR, LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), MissingPeriodHandling.OMIT
        );

        var normalized = normalizer.normalize(result, range);

        assertThat(normalized.data()).hasSize(1);
    }

    private static Object valueFor(IndicatorResult result, String period) {
        return result.data().stream()
            .filter(dp -> period.equals(dp.dimensions().get(TimeSeriesNormalizer.PERIOD_DIMENSION)))
            .findFirst()
            .orElseThrow()
            .value();
    }

    private static Object valueForCountry(IndicatorResult result, String country, String period) {
        return result.data().stream()
            .filter(dp -> country.equals(dp.dimensions().get("country"))
                && period.equals(dp.dimensions().get(TimeSeriesNormalizer.PERIOD_DIMENSION)))
            .findFirst()
            .orElseThrow()
            .value();
    }
}
