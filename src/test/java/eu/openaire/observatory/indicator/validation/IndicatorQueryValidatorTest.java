package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.IndicatorFixtures;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndicatorQueryValidatorTest {

    private static final String COUNT_CODE = "publications.count";
    private static final String RATIO_CODE = "publications.acceptance_rate";
    private static final String NON_TIME_SERIES_CODE = "country.name";
    private static final String RESTRICTED_CODE = "publications.count.restricted";

    private final IndicatorDefinition countIndicator = IndicatorFixtures.countIndicator(COUNT_CODE);
    private final IndicatorDefinition ratioIndicator = IndicatorFixtures.ratioIndicator(RATIO_CODE);
    private final IndicatorDefinition nonTimeSeriesIndicator = IndicatorFixtures.nonTimeSeriesIndicator(NON_TIME_SERIES_CODE);
    private final IndicatorDefinition restrictedIndicator = IndicatorFixtures.restrictedIndicator(RESTRICTED_CODE);
    private final DimensionDefinition countryDimension = IndicatorFixtures.countryDimension();

    private final Map<String, IndicatorDefinition> definitionsByCode = List.of(
        countIndicator, ratioIndicator, nonTimeSeriesIndicator, restrictedIndicator
    ).stream().collect(Collectors.toMap(IndicatorDefinition::code, d -> d));

    private final IndicatorDefinitionLookup definitionLookup = code -> {
        var definition = definitionsByCode.get(code);
        if (definition == null) {
            throw new UnknownIndicatorException(code);
        }
        return definition;
    };

    private final DimensionDefinitionLookup dimensionLookup = code -> {
        if (IndicatorFixtures.COUNTRY_DIMENSION.equals(code)) {
            return countryDimension;
        }
        throw new IllegalStateException("unexpected dimension in test: " + code);
    };

    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        definitionLookup, dimensionLookup, new AllowAllIndicatorAuthorizationService()
    );

    private static final ActorContext ALLOW_ALL = level -> true;
    private static final ActorContext DENY_RESTRICTED = level -> level != IndicatorAccessLevel.RESTRICTED;

    private static IndicatorQuery baseQuery(String code) {
        return new IndicatorQuery(code, null, List.of(), List.of(), null, List.of(), null);
    }

    @Test
    void compileThrowsUnknownIndicatorExceptionForUnregisteredCode() {
        var query = baseQuery("does.not.exist");

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnknownIndicatorException.class);
    }

    @Test
    void compileThrowsAccessDeniedExceptionWhenActorLacksAccess() {
        var query = baseQuery(RESTRICTED_CODE);

        assertThatThrownBy(() -> validator.compile(query, DENY_RESTRICTED))
            .isInstanceOf(IndicatorAccessDeniedException.class);
    }

    @Test
    void compileThrowsInvalidAggregationExceptionWhenAggregationNotAllowed() {
        var query = new IndicatorQuery(
            COUNT_CODE, AggregationType.LAST, List.of(), List.of(), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidAggregationException.class);
    }

    @Test
    void compileSkipsAggregationCheckForRatioIndicators() {
        // NONE is the only allowed aggregation on the ratio fixture; querying without specifying
        // one must not fail even though the default resolves to NONE from the policy.
        var query = baseQuery(RATIO_CODE);

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(RATIO_CODE);
    }

    @Test
    void compileThrowsUnsupportedDimensionExceptionForUnknownGroupByDimension() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of("publicationType"), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    @Test
    void compileThrowsUnsupportedOperatorExceptionForDisallowedFilterOperator() {
        var query = new IndicatorQuery(
            COUNT_CODE,
            null,
            List.of(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.GT, List.of("GR"))),
            List.of(),
            null,
            List.of(),
            null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedOperatorException.class);
    }

    @Test
    void compileThrowsTimeSeriesNotSupportedExceptionWhenIndicatorDoesNotSupportIt() {
        var query = new IndicatorQuery(
            NON_TIME_SERIES_CODE,
            null,
            List.of(),
            List.of(),
            new TimeSeriesRequest(TimeGrain.YEAR, LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), null),
            List.of(),
            null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(TimeSeriesNotSupportedException.class);
    }

    @Test
    void compileProducesExpectedExecutionPlanForValidQuery() {
        var query = new IndicatorQuery(
            COUNT_CODE,
            AggregationType.SUM,
            List.of(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))),
            List.of(IndicatorFixtures.COUNTRY_DIMENSION),
            new TimeSeriesRequest(TimeGrain.YEAR, LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1), null),
            List.of(),
            50
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(COUNT_CODE);
        assertThat(plan.aggregation()).isEqualTo(AggregationType.SUM);
        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorFixtures.COUNTRY_DIMENSION);
        assertThat(plan.timeRange()).isNotNull();
        assertThat(plan.timeRange().missingPeriods())
            .isEqualTo(countIndicator.timePolicy().missingPeriodHandling());
        assertThat(plan.limit()).isEqualTo(50);
        assertThat(plan.securityScope()).isEqualTo(QuerySecurityScope.unrestricted());
    }
}
