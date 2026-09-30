package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.MissingRequiredFilterException;
import eu.openaire.observatory.indicator.validation.MissingTimeScopeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the catalog wiring for the survey-stats-tool-backed indicators: oa.country_initiative_status,
 * oa.eu_country_coverage, and oa.country_policy_status all require an initiativeType filter (there
 * is no meaningful value without picking which initiative the status/percentage refers to);
 * repositories.long_term_preservation_count, citizen_science.new_projects_count, and
 * software.open_source_sets_count are plain country-filterable/groupable counts, the same shape as
 * oa.financial_investment. Every survey-stats-tool indicator supports a real multi-year trend, so
 * surveyYear is an optional filter and a timeSeries range is accepted — but a query carrying
 * neither is a 400 (MissingTimeScopeException): there's no "latest survey" concept to default to.
 */
class IndicatorDefinitionCatalogSurveyStatsToolWiringTest {

    private static final IndicatorFilter SURVEY_YEAR_2023 =
        new IndicatorFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2023"));

    private final IndicatorDefinitionCatalog catalog = new IndicatorDefinitionCatalog();
    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        new CatalogIndicatorDefinitionLookup(catalog), new CatalogDimensionDefinitionLookup(catalog),
        new AllowAllIndicatorAuthorizationService(), new IndicatorQueryHandlerRegistry(List.of()),
        new DimensionMemberProviderRegistry(List.of())
    );
    private static final ActorContext ALLOW_ALL = level -> true;

    @Test
    void countryInitiativeStatusRequiresTheInitiativeTypeFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.COUNTRY_INITIATIVE_STATUS)
            .withFilters(SURVEY_YEAR_2023)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void euCountryCoverageRequiresTheInitiativeTypeFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.EU_COUNTRY_COVERAGE)
            .withFilters(SURVEY_YEAR_2023)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void euCountryCoverageAcceptsAnOptionalCountryFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.EU_COUNTRY_COVERAGE)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")),
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")),
                SURVEY_YEAR_2023
            )
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).extracting("dimensionCode").contains(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void euCountryCoverageAcceptsAnOptionalCountryGroupBy() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.EU_COUNTRY_COVERAGE)
            .withFilters(new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")), SURVEY_YEAR_2023)
            .withGroupBy(IndicatorCodes.COUNTRY_DIMENSION)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void countryInitiativeStatusWithInitiativeTypeAndSurveyYearFiltersValidates() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.COUNTRY_INITIATIVE_STATUS)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")),
                SURVEY_YEAR_2023
            )
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(2);
        assertThat(plan.filters()).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.COUNTRY_POLICY_STATUS})
    void perCountryStatusIndicatorsAcceptAnInitiativeTypeInFilterPlusGroupBy(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN,
                    List.of("OA_PUBLICATION_POLICY", "OPEN_DATA_POLICY")),
                SURVEY_YEAR_2023
            )
            .withGroupBy(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.COUNTRY_POLICY_STATUS})
    void aTimeScopeIsRequiredOnTheTwoPerCountryStatusIndicators(String indicatorCode) {
        // surveyYear is optional now, but a query with neither it nor a timeSeries range has no
        // year to resolve — rejected as a 400 rather than left to blow up in the handler.
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingTimeScopeException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.MONITORED_AREAS_COUNT, IndicatorCodes.POLICY_AREAS_COUNT})
    void aTimeScopeIsRequiredOnTheTwoAreasCountIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode).build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingTimeScopeException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.COUNTRY_POLICY_STATUS})
    void aSurveyYearFilterAloneStillSatisfiesTheTwoPerCountryStatusIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")),
                SURVEY_YEAR_2023
            )
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.timeRange()).isNull();
        assertThat(plan.filters()).extracting("dimensionCode").contains(IndicatorCodes.SURVEY_YEAR_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT,
        IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT
    })
    void surveyYearFilterIsOptionalOnTheThreeTrendCapableCountIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).isEmpty();
        assertThat(plan.timeRange()).isNotNull();
    }

    @Test
    void euCountryCoverageSurveyYearFilterIsOptionalAndTimeSeriesIsAccepted() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.EU_COUNTRY_COVERAGE)
            .withFilters(new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")))
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.timeRange()).isNotNull();
    }

    @Test
    void investmentAreaIsOptionalOnFinancialInvestment() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.FINANCIAL_INVESTMENT)
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();
        var plan = validator.compile(query, ALLOW_ALL);
        assertThat(plan.filters()).isEmpty();
        assertThat(plan.timeRange()).isNotNull();

        var scopedQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.FINANCIAL_INVESTMENT)
            .withFilters(new IndicatorFilter(IndicatorCodes.INVESTMENT_AREA_DIMENSION, FilterOperator.EQ, List.of("OPEN_DATA")))
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();
        var scopedPlan = validator.compile(scopedQuery, ALLOW_ALL);

        assertThat(scopedPlan.filters()).extracting("dimensionCode").containsExactly(IndicatorCodes.INVESTMENT_AREA_DIMENSION);
        assertThat(scopedPlan.timeRange()).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.COUNTRY_POLICY_STATUS})
    void timeSeriesIsAcceptedOnTheTwoPerCountryStatusIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_MONITORING_INITIATIVE")))
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.timeRange().from()).isEqualTo(2022);
        assertThat(plan.timeRange().to()).isEqualTo(2023);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.MONITORED_AREAS_COUNT, IndicatorCodes.POLICY_AREAS_COUNT})
    void timeSeriesIsAcceptedOnTheTwoAreasCountIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.timeRange()).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT,
        IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT
    })
    void timeSeriesIsAcceptedOnTheThreeTrendCapableCountIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.timeRange().from()).isEqualTo(2022);
        assertThat(plan.timeRange().to()).isEqualTo(2023);
    }

    @Test
    void allSurveyIndicatorsRouteToTheSurveyStatsToolHandler() {
        for (String code : List.of(
                IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.EU_COUNTRY_COVERAGE,
                IndicatorCodes.COUNTRY_POLICY_STATUS, IndicatorCodes.MONITORED_AREAS_COUNT, IndicatorCodes.POLICY_AREAS_COUNT,
                IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT,
                IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT)) {
            assertThat(catalog.get(code).executionBinding().handlerKey())
                .as("handler for %s", code)
                .isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
        }
    }

    @Test
    void financialInvestmentAcceptsAnOptionalCountryFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.FINANCIAL_INVESTMENT)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")),
                new IndicatorFilter(IndicatorCodes.INVESTMENT_AREA_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION")),
                SURVEY_YEAR_2023
            )
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).extracting("dimensionCode").contains(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void longTermPreservationCountAcceptsAnOptionalCountryFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.LONG_TERM_PRESERVATION_COUNT)
            .withFilters(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("PL")), SURVEY_YEAR_2023)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).extracting("dimensionCode").contains(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void citizenScienceNewProjectsCountAcceptsAnOptionalCountryGroupBy() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT)
            .withFilters(SURVEY_YEAR_2023)
            .withGroupBy(IndicatorCodes.COUNTRY_DIMENSION)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }
}
