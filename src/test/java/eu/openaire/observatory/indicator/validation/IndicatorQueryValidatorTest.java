package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.IndicatorFixtures;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.handler.SupportsComparisonQueries;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import org.junit.jupiter.api.Test;

import java.time.Year;
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
    private static final String REQUIRED_FILTER_CODE = "publications.count.required_filter";
    private static final String COMPARABLE_RATIO_CODE = "publications.access_status_share";
    private static final String COMPARABLE_ATTRIBUTE_CODE = "country.name.timeseries";
    private static final String NON_COMPARABLE_COUNT_CODE = "publications.count.no_comparison_handler";

    private final IndicatorDefinition countIndicator = IndicatorFixtures.countIndicator(COUNT_CODE);
    private final IndicatorDefinition ratioIndicator = IndicatorFixtures.ratioIndicator(RATIO_CODE);
    private final IndicatorDefinition nonTimeSeriesIndicator = IndicatorFixtures.nonTimeSeriesIndicator(NON_TIME_SERIES_CODE);
    private final IndicatorDefinition restrictedIndicator = IndicatorFixtures.restrictedIndicator(RESTRICTED_CODE);
    private final IndicatorDefinition requiredFilterIndicator = IndicatorFixtures.requiredFilterIndicator(REQUIRED_FILTER_CODE);
    private final IndicatorDefinition comparableRatioIndicator = IndicatorFixtures.timeSeriesRatioIndicator(COMPARABLE_RATIO_CODE);
    private final IndicatorDefinition comparableAttributeIndicator =
        IndicatorFixtures.timeSeriesAttributeIndicator(COMPARABLE_ATTRIBUTE_CODE);
    private final IndicatorDefinition nonComparableCountIndicator = IndicatorFixtures.countIndicator(NON_COMPARABLE_COUNT_CODE);
    private final DimensionDefinition countryDimension = IndicatorFixtures.countryDimension();

    private final Map<String, IndicatorDefinition> definitionsByCode = List.of(
        countIndicator, ratioIndicator, nonTimeSeriesIndicator, restrictedIndicator, requiredFilterIndicator,
        comparableRatioIndicator, comparableAttributeIndicator, nonComparableCountIndicator
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

    /** handlerKey defaults to the indicator's own code (IndicatorFixtures), so each handler below is keyed by code. */
    private final IndicatorQueryHandlerRegistry handlers = new IndicatorQueryHandlerRegistry(List.of(
        new ComparisonCapableHandler(COUNT_CODE),
        new ComparisonCapableHandler(COMPARABLE_RATIO_CODE),
        new ComparisonCapableHandler(COMPARABLE_ATTRIBUTE_CODE),
        new PlainHandler(NON_COMPARABLE_COUNT_CODE)
    ));

    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        definitionLookup, dimensionLookup, new AllowAllIndicatorAuthorizationService(), handlers
    );

    private static final ActorContext ALLOW_ALL = level -> true;
    private static final ActorContext DENY_RESTRICTED = level -> level != IndicatorAccessLevel.RESTRICTED;

    private static IndicatorQuery baseQuery(String code) {
        return new IndicatorQuery(code, null, List.of(), List.of(), null, null, List.of(), null);
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
            COUNT_CODE, AggregationType.LAST, List.of(), List.of(), null, null, List.of(), null
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
            COUNT_CODE, null, List.of(), List.of("publicationType"), null, null, List.of(), null
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
            new TimeSeriesRequest(2020, 2022, null),
            null,
            List.of(),
            null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(TimeSeriesNotSupportedException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenFromBeforeEarliestSupportedYear() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(), new TimeSeriesRequest(2014, 2020, null), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenFromAfterTo() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(), new TimeSeriesRequest(2022, 2020, null), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenToAfterCurrentYear() {
        int nextYear = Year.now().getValue() + 1;
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(), new TimeSeriesRequest(2020, nextYear, null), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenFromIsMissing() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(), new TimeSeriesRequest(null, 2020, null), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenToIsMissing() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(), new TimeSeriesRequest(2020, null, null), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsMissingRequiredFilterExceptionWhenRequiredFilterOmitted() {
        var query = baseQuery(REQUIRED_FILTER_CODE);

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void compileSucceedsWhenRequiredFilterIsProvided() {
        var query = new IndicatorQuery(
            REQUIRED_FILTER_CODE,
            null,
            List.of(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))),
            List.of(),
            null,
            null,
            List.of(),
            null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(REQUIRED_FILTER_CODE);
        assertThat(plan.filters()).hasSize(1);
    }

    @Test
    void compileProducesExpectedExecutionPlanForValidQuery() {
        var query = new IndicatorQuery(
            COUNT_CODE,
            AggregationType.SUM,
            List.of(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))),
            List.of(IndicatorFixtures.COUNTRY_DIMENSION),
            new TimeSeriesRequest(2020, 2022, null),
            null,
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
        assertThat(plan.comparisonMode()).isNull();
        assertThat(plan.limit()).isEqualTo(50);
        assertThat(plan.securityScope()).isEqualTo(QuerySecurityScope.unrestricted());
    }

    @Test
    void compileAcceptsComparisonModeForMeasureIndicatorWithCapableHandler() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(),
            new TimeSeriesRequest(2020, 2022, null), ComparisonMode.TWO_POINT, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.comparisonMode()).isEqualTo(ComparisonMode.TWO_POINT);
    }

    @Test
    void compileAcceptsComparisonModeForRatioIndicatorWithCapableHandler() {
        var query = new IndicatorQuery(
            COMPARABLE_RATIO_CODE, null, List.of(), List.of(),
            new TimeSeriesRequest(2020, 2022, null), ComparisonMode.CONSECUTIVE, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.comparisonMode()).isEqualTo(ComparisonMode.CONSECUTIVE);
    }

    @Test
    void compileThrowsComparisonRequiresTimeSeriesExceptionWhenTimeSeriesOmitted() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(), null, ComparisonMode.TWO_POINT, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(ComparisonRequiresTimeSeriesException.class);
    }

    @Test
    void compileThrowsInvalidComparisonRangeExceptionWhenFromEqualsTo() {
        var query = new IndicatorQuery(
            COUNT_CODE, null, List.of(), List.of(),
            new TimeSeriesRequest(2022, 2022, null), ComparisonMode.TWO_POINT, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidComparisonRangeException.class);
    }

    @Test
    void compileThrowsComparisonNotSupportedExceptionForAttributeIndicators() {
        var query = new IndicatorQuery(
            COMPARABLE_ATTRIBUTE_CODE, null, List.of(), List.of(),
            new TimeSeriesRequest(2020, 2022, null), ComparisonMode.TWO_POINT, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(ComparisonNotSupportedException.class);
    }

    @Test
    void compileThrowsComparisonNotSupportedExceptionWhenHandlerLacksTheMarker() {
        var query = new IndicatorQuery(
            NON_COMPARABLE_COUNT_CODE, null, List.of(), List.of(),
            new TimeSeriesRequest(2020, 2022, null), ComparisonMode.TWO_POINT, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(ComparisonNotSupportedException.class);
    }

    private static final class ComparisonCapableHandler implements IndicatorQueryHandler, SupportsComparisonQueries {
        private final String handlerKey;

        private ComparisonCapableHandler(String handlerKey) {
            this.handlerKey = handlerKey;
        }

        @Override
        public String handlerKey() {
            return handlerKey;
        }

        @Override
        public IndicatorResult execute(IndicatorExecutionPlan plan) {
            throw new UnsupportedOperationException("not exercised by validator tests");
        }
    }

    private static final class PlainHandler implements IndicatorQueryHandler {
        private final String handlerKey;

        private PlainHandler(String handlerKey) {
            this.handlerKey = handlerKey;
        }

        @Override
        public String handlerKey() {
            return handlerKey;
        }

        @Override
        public IndicatorResult execute(IndicatorExecutionPlan plan) {
            throw new UnsupportedOperationException("not exercised by validator tests");
        }
    }
}
