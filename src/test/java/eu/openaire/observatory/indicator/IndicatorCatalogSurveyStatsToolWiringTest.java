package eu.openaire.observatory.indicator;

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
import eu.openaire.observatory.indicator.validation.TimeSeriesNotSupportedException;
import eu.openaire.observatory.indicator.validation.UnsupportedDimensionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the catalog wiring for the 5 survey-stats-tool-backed indicators:
 * oa.country_initiative_status and oa.eu_country_coverage both require an initiativeType filter
 * (there is no meaningful value without picking which initiative the Yes/No/percentage refers
 * to); repositories.long_term_preservation_count and citizen_science.new_projects_count are
 * plain country-filterable/groupable counts, the same shape as oa.financial_investment. All 5
 * route to the survey handler, not the OpenAIRE one, and all 5 also require a surveyYear filter
 * (there's no "latest survey" concept to default to) and reject timeSeries outright.
 */
class IndicatorCatalogSurveyStatsToolWiringTest {

    private static final IndicatorFilter SURVEY_YEAR_2023 =
        new IndicatorFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2023"));

    private final IndicatorCatalog catalog = new IndicatorCatalog();
    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        new CatalogIndicatorDefinitionLookup(catalog), new CatalogDimensionDefinitionLookup(catalog),
        new AllowAllIndicatorAuthorizationService(), new IndicatorQueryHandlerRegistry(List.of())
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
    void euCountryCoverageCountryIsNotClientFilterable() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.EU_COUNTRY_COVERAGE)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")),
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("MONITORING_INITIATIVE")),
                SURVEY_YEAR_2023
            )
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    @Test
    void countryInitiativeStatusWithInitiativeTypeAndSurveyYearFiltersValidates() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.COUNTRY_INITIATIVE_STATUS)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("MONITORING_INITIATIVE")),
                SURVEY_YEAR_2023
            )
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(2);
        assertThat(plan.filters()).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.EU_COUNTRY_COVERAGE,
        IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT
    })
    void surveyYearFilterIsRequiredOnAllFiveSurveyIndicators(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode).build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.EU_COUNTRY_COVERAGE,
        IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT
    })
    void timeSeriesIsRejectedOnAllFiveSurveyIndicators(String indicatorCode) {
        // All required filters must be present too, so the query reaches timeSeries validation
        // instead of failing earlier on a missing required filter.
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(requiredFiltersFor(indicatorCode))
            .withTimeSeries(new TimeSeriesRequest(2022, 2023, null))
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(TimeSeriesNotSupportedException.class);
    }

    private static IndicatorFilter[] requiredFiltersFor(String indicatorCode) {
        boolean needsInitiativeType = indicatorCode.equals(IndicatorCodes.COUNTRY_INITIATIVE_STATUS)
            || indicatorCode.equals(IndicatorCodes.EU_COUNTRY_COVERAGE);
        return needsInitiativeType
            ? new IndicatorFilter[]{
                new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("MONITORING_INITIATIVE")),
                SURVEY_YEAR_2023
            }
            : new IndicatorFilter[]{SURVEY_YEAR_2023};
    }

    @Test
    void allFiveSurveyIndicatorsRouteToTheSurveyStatsToolHandler() {
        assertThat(catalog.get(IndicatorCodes.FINANCIAL_INVESTMENT).executionBinding().handlerKey())
            .isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
        assertThat(catalog.get(IndicatorCodes.COUNTRY_INITIATIVE_STATUS).executionBinding().handlerKey())
            .isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
        assertThat(catalog.get(IndicatorCodes.EU_COUNTRY_COVERAGE).executionBinding().handlerKey())
            .isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
        assertThat(catalog.get(IndicatorCodes.LONG_TERM_PRESERVATION_COUNT).executionBinding().handlerKey())
            .isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
        assertThat(catalog.get(IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT).executionBinding().handlerKey())
            .isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void financialInvestmentAcceptsAnOptionalCountryFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.FINANCIAL_INVESTMENT)
            .withFilters(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")), SURVEY_YEAR_2023)
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
