package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.IndicatorFixtures;
import eu.openaire.observatory.indicator.dimension.DimensionMember;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProvider;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.dimension.DimensionMembers;
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
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
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
    private static final String STATUS_FILTERABLE_CODE = "publications.count.status_filterable";
    private static final String SURVEY_YEAR_FILTERABLE_CODE = "survey.backed_measure";

    private final IndicatorDefinition countIndicator = IndicatorFixtures.countIndicator(COUNT_CODE);
    private final IndicatorDefinition ratioIndicator = IndicatorFixtures.ratioIndicator(RATIO_CODE);
    private final IndicatorDefinition nonTimeSeriesIndicator = IndicatorFixtures.nonTimeSeriesIndicator(NON_TIME_SERIES_CODE);
    private final IndicatorDefinition restrictedIndicator = IndicatorFixtures.restrictedIndicator(RESTRICTED_CODE);
    private final IndicatorDefinition requiredFilterIndicator = IndicatorFixtures.requiredFilterIndicator(REQUIRED_FILTER_CODE);
    private final IndicatorDefinition comparableRatioIndicator = IndicatorFixtures.timeSeriesRatioIndicator(COMPARABLE_RATIO_CODE);
    private final IndicatorDefinition comparableAttributeIndicator =
        IndicatorFixtures.timeSeriesAttributeIndicator(COMPARABLE_ATTRIBUTE_CODE);
    private final IndicatorDefinition nonComparableCountIndicator = IndicatorFixtures.countIndicator(NON_COMPARABLE_COUNT_CODE);
    private final IndicatorDefinition statusFilterableIndicator = IndicatorFixtures.statusFilterableIndicator(STATUS_FILTERABLE_CODE);
    private final IndicatorDefinition surveyYearFilterableIndicator =
        IndicatorFixtures.surveyYearFilterableIndicator(SURVEY_YEAR_FILTERABLE_CODE);
    private final DimensionDefinition countryDimension = IndicatorFixtures.countryDimension();

    private final Map<String, IndicatorDefinition> definitionsByCode = List.of(
        countIndicator, ratioIndicator, nonTimeSeriesIndicator, restrictedIndicator, requiredFilterIndicator,
        comparableRatioIndicator, comparableAttributeIndicator, nonComparableCountIndicator, statusFilterableIndicator,
        surveyYearFilterableIndicator
    ).stream().collect(Collectors.toMap(IndicatorDefinition::code, d -> d));

    private final IndicatorDefinitionLookup definitionLookup = code -> {
        var definition = definitionsByCode.get(code);
        if (definition == null) {
            throw new UnknownIndicatorException(code);
        }
        return definition;
    };

    private final DimensionDefinition statusDimension = IndicatorFixtures.statusDimension();
    private final DimensionDefinition surveyYearDimension = IndicatorFixtures.surveyYearDimension();

    private final DimensionDefinitionLookup dimensionLookup = code -> {
        if (IndicatorFixtures.COUNTRY_DIMENSION.equals(code)) {
            return countryDimension;
        }
        if (IndicatorFixtures.STATUS_DIMENSION.equals(code)) {
            return statusDimension;
        }
        if (IndicatorFixtures.SURVEY_YEAR_DIMENSION.equals(code)) {
            return surveyYearDimension;
        }
        throw new IllegalStateException("unexpected dimension in test: " + code);
    };

    /**
     * Fixture provider: only ACTIVE/INACTIVE are valid "status" filter values. Doesn't override
     * validCodes() — exercises DimensionMemberProvider's default implementation (derived from
     * search()) through the validator, not just a hand-rolled test double.
     */
    private static final class StatusDimensionMemberProvider implements DimensionMemberProvider {
        @Override
        public String dimensionCode() {
            return IndicatorFixtures.STATUS_DIMENSION;
        }

        @Override
        public DimensionMembers search(String indicatorCode, String searchText, int limit) {
            return new DimensionMembers(List.of(
                new DimensionMember("ACTIVE", "Active"),
                new DimensionMember("INACTIVE", "Inactive")
            ));
        }
    }

    /**
     * Fixture provider: knows only about 2023. The validator must NOT gate a surveyYear filter on
     * this list (unlike a closed enumeration) — a year the survey stats-tool lacks still compiles.
     */
    private static final class SurveyYearDimensionMemberProvider implements DimensionMemberProvider {
        @Override
        public String dimensionCode() {
            return IndicatorFixtures.SURVEY_YEAR_DIMENSION;
        }

        @Override
        public DimensionMembers search(String indicatorCode, String searchText, int limit) {
            return new DimensionMembers(List.of(new DimensionMember("2023", "2023")));
        }
    }

    private final DimensionMemberProviderRegistry memberProviders =
        new DimensionMemberProviderRegistry(List.of(new StatusDimensionMemberProvider(), new SurveyYearDimensionMemberProvider()));

    /** handlerKey defaults to the indicator's own code (IndicatorFixtures), so each handler below is keyed by code. */
    private final IndicatorQueryHandlerRegistry handlers = new IndicatorQueryHandlerRegistry(List.of(
        new ComparisonCapableHandler(COUNT_CODE),
        new ComparisonCapableHandler(COMPARABLE_RATIO_CODE),
        new ComparisonCapableHandler(COMPARABLE_ATTRIBUTE_CODE),
        new PlainHandler(NON_COMPARABLE_COUNT_CODE)
    ));

    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        definitionLookup, dimensionLookup, new AllowAllIndicatorAuthorizationService(), handlers, memberProviders
    );

    private static final ActorContext ALLOW_ALL = level -> true;
    private static final ActorContext DENY_RESTRICTED = level -> level != IndicatorAccessLevel.RESTRICTED;

    private static IndicatorQuery baseQuery(String code) {
        return IndicatorQueryBuilder.forIndicator(code).build();
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
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withAggregation(AggregationType.LAST)
            .build();

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
    void compileDefaultsSeriesAggregationsToEmptyListWhenOmitted() {
        var query = baseQuery(COUNT_CODE);

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.seriesAggregations()).isEmpty();
    }

    @Test
    void compileResolvesSeriesAggregationsOntoThePlan() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withSeriesAggregations(AggregationType.AVG, AggregationType.MAX)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.seriesAggregations()).containsExactly(AggregationType.AVG, AggregationType.MAX);
    }

    @Test
    void compileThrowsInvalidAggregationExceptionWhenSeriesAggregationNotAllowed() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withSeriesAggregations(AggregationType.MEDIAN)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidAggregationException.class);
    }

    @Test
    void compileThrowsInvalidAggregationExceptionWhenSeriesAggregationIsNone() {
        var query = IndicatorQueryBuilder.forIndicator(RATIO_CODE)
            .withSeriesAggregations(AggregationType.NONE)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidAggregationException.class);
    }

    @Test
    void compileThrowsUnsupportedDimensionExceptionForUnknownGroupByDimension() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withGroupBy("publicationType")
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    @Test
    void compileThrowsUnsupportedOperatorExceptionForDisallowedFilterOperator() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.GT, List.of("GR")))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedOperatorException.class);
    }

    @Test
    void compileThrowsInvalidFilterValueExceptionForUnknownValueOnClosedEnumerationDimension() {
        var query = IndicatorQueryBuilder.forIndicator(STATUS_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.STATUS_DIMENSION, FilterOperator.EQ, List.of("NOT_A_REAL_STATUS")))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidFilterValueException.class);
    }

    @Test
    void compileThrowsInvalidFilterValueExceptionWhenOneOfSeveralInValuesIsUnknown() {
        var query = IndicatorQueryBuilder.forIndicator(STATUS_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.STATUS_DIMENSION, FilterOperator.IN, List.of("ACTIVE", "NOT_A_REAL_STATUS")))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidFilterValueException.class);
    }

    @Test
    void compileSucceedsForKnownValueOnClosedEnumerationDimension() {
        var query = IndicatorQueryBuilder.forIndicator(STATUS_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.STATUS_DIMENSION, FilterOperator.EQ, List.of("ACTIVE")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
    }

    @Test
    void compileDoesNotValidateFilterValuesWhenNoDimensionMemberProviderIsRegistered() {
        // "country" has no registered DimensionMemberProvider in this test's minimal registry (unlike
        // production, where CountryDimensionMemberProvider IS registered and would validate this) —
        // there's nothing to check a value against, so it must not be rejected.
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("NOT_A_REAL_COUNTRY")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
    }

    @Test
    void compileAcceptsASurveyYearOutsideTheProvidersMemberList() {
        // The provider knows only 2023; a dashboard configured for an earlier year must still compile,
        // degrading to null data downstream rather than being rejected here.
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2019")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
    }

    @Test
    void compileAcceptsTheCurrentYearAsASurveyYearEvenWithNoSurveyForItYet() {
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(
                IndicatorFixtures.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of(String.valueOf(Year.now().getValue()))))
            .build();

        assertThat(validator.compile(query, ALLOW_ALL).filters()).hasSize(1);
    }

    @Test
    void compileRejectsANonIntegerSurveyYear() {
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("not-a-year")))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidFilterValueException.class);
    }

    @Test
    void compileRejectsASurveyYearBelowTheEarliestSupportedYear() {
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2014")))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidFilterValueException.class);
    }

    @Test
    void compileRejectsASurveyYearAfterTheCurrentYear() {
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE)
            .withFilters(new IndicatorFilter(
                IndicatorFixtures.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of(String.valueOf(Year.now().getValue() + 1))))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidFilterValueException.class);
    }

    @Test
    void compileRejectsASurveyBackedIndicatorWithNeitherATimeSeriesNorASurveyYearFilter() {
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE).build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingTimeScopeException.class);
    }

    @Test
    void compileAcceptsASurveyBackedIndicatorWithATimeSeriesAndNoSurveyYearFilter() {
        var query = IndicatorQueryBuilder.forIndicator(SURVEY_YEAR_FILTERABLE_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, 2023, null))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.timeRange()).isNotNull();
        assertThat(plan.filters()).isEmpty();
    }

    @Test
    void compileThrowsTimeSeriesNotSupportedExceptionWhenIndicatorDoesNotSupportIt() {
        var query = IndicatorQueryBuilder.forIndicator(NON_TIME_SERIES_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, 2022, null))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(TimeSeriesNotSupportedException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenFromBeforeEarliestSupportedYear() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2014, 2020, null))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenFromAfterTo() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2022, 2020, null))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenToAfterCurrentYear() {
        int nextYear = Year.now().getValue() + 1;
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, nextYear, null))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenFromIsMissing() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(null, 2020, null))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidTimeRangeException.class);
    }

    @Test
    void compileThrowsInvalidTimeRangeExceptionWhenToIsMissing() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, null, null))
            .build();

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
        var query = IndicatorQueryBuilder.forIndicator(REQUIRED_FILTER_CODE)
            .withFilters(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(REQUIRED_FILTER_CODE);
        assertThat(plan.filters()).hasSize(1);
    }

    @Test
    void compileProducesExpectedExecutionPlanForValidQuery() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withAggregation(AggregationType.SUM)
            .withFilters(new IndicatorFilter(IndicatorFixtures.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")))
            .withGroupBy(IndicatorFixtures.COUNTRY_DIMENSION)
            .withTimeSeries(new TimeSeriesRequest(2020, 2022, null))
            .withLimit(50)
            .build();

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
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, 2022, null))
            .withComparisonMode(ComparisonMode.TWO_POINT)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.comparisonMode()).isEqualTo(ComparisonMode.TWO_POINT);
    }

    @Test
    void compileAcceptsComparisonModeForRatioIndicatorWithCapableHandler() {
        var query = IndicatorQueryBuilder.forIndicator(COMPARABLE_RATIO_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, 2022, null))
            .withComparisonMode(ComparisonMode.CONSECUTIVE)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.comparisonMode()).isEqualTo(ComparisonMode.CONSECUTIVE);
    }

    @Test
    void compileThrowsComparisonRequiresTimeSeriesExceptionWhenTimeSeriesOmitted() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withComparisonMode(ComparisonMode.TWO_POINT)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(ComparisonRequiresTimeSeriesException.class);
    }

    @Test
    void compileThrowsInvalidComparisonRangeExceptionWhenFromEqualsTo() {
        var query = IndicatorQueryBuilder.forIndicator(COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2022, 2022, null))
            .withComparisonMode(ComparisonMode.TWO_POINT)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(InvalidComparisonRangeException.class);
    }

    @Test
    void compileThrowsComparisonNotSupportedExceptionForAttributeIndicators() {
        var query = IndicatorQueryBuilder.forIndicator(COMPARABLE_ATTRIBUTE_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, 2022, null))
            .withComparisonMode(ComparisonMode.TWO_POINT)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(ComparisonNotSupportedException.class);
    }

    @Test
    void compileThrowsComparisonNotSupportedExceptionWhenHandlerLacksTheMarker() {
        var query = IndicatorQueryBuilder.forIndicator(NON_COMPARABLE_COUNT_CODE)
            .withTimeSeries(new TimeSeriesRequest(2020, 2022, null))
            .withComparisonMode(ComparisonMode.TWO_POINT)
            .build();

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
