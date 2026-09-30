package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProvider;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.dimension.DimensionMembers;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorExclusion;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSeriesNormalizerTest {

    private final TimeSeriesNormalizer normalizer = new TimeSeriesNormalizer(new DimensionMemberProviderRegistry(List.of()));

    private static DimensionMemberProvider fakeProvider(String dimensionCode, String... codes) {
        return new DimensionMemberProvider() {
            @Override
            public String dimensionCode() {
                return dimensionCode;
            }

            @Override
            public DimensionMembers search(String indicatorCode, String searchText, int limit) {
                return new DimensionMembers(List.of(codes).stream().map(code -> new DimensionMember(code, code)).toList());
            }
        };
    }

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
        var range = new ResolvedTimeRange(2020, 2022, MissingPeriodHandling.ZERO_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(normalized.data()).hasSize(3);
        assertThat(valueFor(normalized, "2020")).isEqualTo(10);
        assertThat(valueFor(normalized, "2021")).isEqualTo(0);
        assertThat(valueFor(normalized, "2022")).isEqualTo(20);
    }

    @Test
    void aMissingYearListedAsExcludedStaysNullUnderZeroFill() {
        var base = resultWithPeriods(Map.of("2020", 10, "2022", 20));
        var result = new IndicatorResult(base.metadata(), base.dimensions(), base.data(), base.execution(),
            List.of(), List.of(IndicatorExclusion.questionPaused(2021, "investmentArea", "SERVICES")));
        var range = new ResolvedTimeRange(2020, 2022, MissingPeriodHandling.ZERO_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(valueFor(normalized, "2021")).isNull();
        assertThat(normalized.exclusions()).hasSize(1);
    }

    @Test
    void anExclusionOnlyKeepsTheGroupItNamesNullUnderZeroFill() {
        var metadata = new IndicatorMetadata("oa.financial_investment", "label", null, null, null);
        var data = List.of(
            new IndicatorDataPoint(Map.of("investmentArea", "OPEN_DATA", "period", "2020"), 5),
            new IndicatorDataPoint(Map.of("investmentArea", "SERVICES", "period", "2020"), 7));
        var result = new IndicatorResult(metadata, List.of("investmentArea", "period"), data,
            new QueryExecutionMetadata(Instant.now(), false), List.of(),
            List.of(IndicatorExclusion.questionPaused(2021, "investmentArea", "SERVICES")));
        var range = new ResolvedTimeRange(2020, 2021, MissingPeriodHandling.ZERO_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(valueFor(normalized, "investmentArea", "OPEN_DATA", "2021")).isEqualTo(0);
        assertThat(valueFor(normalized, "investmentArea", "SERVICES", "2021")).isNull();
    }

    @Test
    void aWholeValueExclusionKeepsEveryGroupNullUnderZeroFill() {
        var metadata = new IndicatorMetadata("count", "label", null, null, null);
        var data = List.of(new IndicatorDataPoint(Map.of("country", "GR", "period", "2020"), 5));
        var result = new IndicatorResult(metadata, List.of("country", "period"), data,
            new QueryExecutionMetadata(Instant.now(), false), List.of(),
            List.of(IndicatorExclusion.questionPaused(2021)));
        var range = new ResolvedTimeRange(2020, 2021, MissingPeriodHandling.ZERO_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(valueFor(normalized, "country", "GR", "2021")).isNull();
    }

    @Test
    void nullFillsMissingYears() {
        var result = resultWithPeriods(Map.of("2020", 10));
        var range = new ResolvedTimeRange(2020, 2021, MissingPeriodHandling.NULL_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

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
        var range = new ResolvedTimeRange(2020, 2022, MissingPeriodHandling.ZERO_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(normalized.data()).hasSize(6);
        assertThat(valueForCountry(normalized, "GR", "2020")).isEqualTo(10);
        assertThat(valueForCountry(normalized, "GR", "2021")).isEqualTo(0);
        assertThat(valueForCountry(normalized, "GR", "2022")).isEqualTo(30);
        assertThat(valueForCountry(normalized, "DE", "2020")).isEqualTo(0);
        assertThat(valueForCountry(normalized, "DE", "2021")).isEqualTo(20);
        assertThat(valueForCountry(normalized, "DE", "2022")).isEqualTo(0);
    }

    @Test
    void nullFillsEachCountrysStatusSeriesIndependently() {
        // A per-country ATTRIBUTE series (oa.country_initiative_status &c.): a year with no survey
        // data for a country comes back as a null status, not dropped and not a fabricated value.
        List<IndicatorDataPoint> data = List.of(
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2022"), "NO"),
            new IndicatorDataPoint(Map.of("country", "GR", TimeSeriesNormalizer.PERIOD_DIMENSION, "2024"), "YES"),
            new IndicatorDataPoint(Map.of("country", "DE", TimeSeriesNormalizer.PERIOD_DIMENSION, "2023"), "YES")
        );
        var result = new IndicatorResult(
            new IndicatorMetadata("oa.country_initiative_status", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            data,
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(2022, 2024, MissingPeriodHandling.NULL_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(normalized.data()).hasSize(6);
        assertThat(valueForCountry(normalized, "GR", "2023")).isNull();
        assertThat(valueForCountry(normalized, "GR", "2024")).isEqualTo("YES");
        assertThat(valueForCountry(normalized, "DE", "2022")).isNull();
        assertThat(valueForCountry(normalized, "DE", "2024")).isNull();
    }

    @Test
    void aggregateWithNoGroupingDimensionStillNullFillsEveryPeriodWhenCompletelyEmpty() {
        var result = resultWithPeriods(Map.of());
        var range = new ResolvedTimeRange(2020, 2021, MissingPeriodHandling.NULL_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(normalized.data()).hasSize(2);
        assertThat(valueFor(normalized, "2020")).isNull();
        assertThat(valueFor(normalized, "2021")).isNull();
    }

    @Test
    void groupedResultWithNoDataAtAllStaysEmptyWhenDimensionHasNoProviderOrFilter() {
        // No registered DimensionMemberProvider for "country" (default `normalizer` has none
        // registered) and no filter narrowing it either: there's no reasonable set of expected
        // groups to fabricate, so the result must stay zero rows — not a phantom point missing
        // the "country" key it declares in dimensions().
        var result = new IndicatorResult(
            new IndicatorMetadata("oa.financial_investment", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            List.of(),
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(2020, 2022, MissingPeriodHandling.NULL_FILL);

        var normalized = normalizer.normalize(result, range, List.of());

        assertThat(normalized.data()).isEmpty();
        assertThat(normalized.dimensions()).containsExactly("country", TimeSeriesNormalizer.PERIOD_DIMENSION);
    }

    @Test
    void groupedResultWithNoDataAtAllIsSeededFromTheRegisteredMemberProvider() {
        var normalizerWithCountries = new TimeSeriesNormalizer(
            new DimensionMemberProviderRegistry(List.of(fakeProvider("country", "DE", "FR"))));
        var result = new IndicatorResult(
            new IndicatorMetadata("citizen_science.new_projects_count", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            List.of(),
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(2024, 2024, MissingPeriodHandling.NULL_FILL);

        var normalized = normalizerWithCountries.normalize(result, range, List.of());

        assertThat(normalized.data()).hasSize(2);
        assertThat(valueForCountry(normalized, "DE", "2024")).isNull();
        assertThat(valueForCountry(normalized, "FR", "2024")).isNull();
    }

    @Test
    void groupedResultWithNoDataAtAllPrefersAnExplicitFilterOverTheMemberProvider() {
        var normalizerWithCountries = new TimeSeriesNormalizer(
            new DimensionMemberProviderRegistry(List.of(fakeProvider("country", "DE", "FR", "PL", "IT"))));
        var result = new IndicatorResult(
            new IndicatorMetadata("citizen_science.new_projects_count", "label", null, null, null),
            List.of("country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            List.of(),
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(2024, 2024, MissingPeriodHandling.NULL_FILL);
        var filters = List.of(new ResolvedFilter("country", FilterOperator.IN, List.of("PL", "IT")));

        var normalized = normalizerWithCountries.normalize(result, range, filters);

        assertThat(normalized.data()).hasSize(2);
        assertThat(valueForCountry(normalized, "PL", "2024")).isNull();
        assertThat(valueForCountry(normalized, "IT", "2024")).isNull();
    }

    @Test
    void groupedResultWithNoDataAtAllStaysEmptyWhenGroupedByMultipleDimensions() {
        var normalizerWithCountries = new TimeSeriesNormalizer(
            new DimensionMemberProviderRegistry(List.of(fakeProvider("country", "DE", "FR"))));
        var result = new IndicatorResult(
            new IndicatorMetadata("publications.oa_license_status_count", "label", null, null, null),
            List.of("accessStatus", "country", TimeSeriesNormalizer.PERIOD_DIMENSION),
            List.of(),
            new QueryExecutionMetadata(Instant.now(), false)
        );
        var range = new ResolvedTimeRange(2020, 2022, MissingPeriodHandling.NULL_FILL);

        var normalized = normalizerWithCountries.normalize(result, range, List.of());

        assertThat(normalized.data()).isEmpty();
    }

    @Test
    void omitLeavesGapsUntouched() {
        var result = resultWithPeriods(Map.of("2020", 10));
        var range = new ResolvedTimeRange(2020, 2022, MissingPeriodHandling.OMIT);

        var normalized = normalizer.normalize(result, range, List.of());

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

    private static Object valueFor(IndicatorResult result, String dimension, String member, String period) {
        return result.data().stream()
            .filter(dp -> member.equals(dp.dimensions().get(dimension)) && period.equals(dp.dimensions().get("period")))
            .findFirst().orElseThrow().value();
    }
}
