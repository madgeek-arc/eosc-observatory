package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.MissingRequiredFilterException;
import eu.openaire.observatory.indicator.validation.UnsupportedDimensionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the catalog wiring for the 5 survey-stats-tool-backed indicators:
 * oa.country_initiative_status and oa.eu_country_coverage both require an initiativeType filter
 * (there is no meaningful value without picking which initiative the Yes/No/percentage refers
 * to); repositories.long_term_preservation_count and citizen_science.new_projects_count are
 * plain country-filterable/groupable counts, the same shape as oa.financial_investment. All 5
 * route to the survey handler, not the OpenAIRE one.
 */
class IndicatorCatalogSurveyStatsToolWiringTest {

    private final IndicatorCatalog catalog = new IndicatorCatalog();
    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        new CatalogIndicatorDefinitionLookup(catalog), new CatalogDimensionDefinitionLookup(catalog),
        new AllowAllIndicatorAuthorizationService(), new IndicatorQueryHandlerRegistry(List.of())
    );
    private static final ActorContext ALLOW_ALL = level -> true;

    @Test
    void countryInitiativeStatusRequiresTheInitiativeTypeFilter() {
        var query = new IndicatorQuery(
            IndicatorCodes.COUNTRY_INITIATIVE_STATUS, null, List.of(), List.of(), null, null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void euCountryCoverageRequiresTheInitiativeTypeFilter() {
        var query = new IndicatorQuery(
            IndicatorCodes.EU_COUNTRY_COVERAGE, null, List.of(), List.of(), null, null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void euCountryCoverageCountryIsNotClientFilterable() {
        var query = new IndicatorQuery(
            IndicatorCodes.EU_COUNTRY_COVERAGE, null,
            List.of(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))),
            List.of(), null, null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    @Test
    void countryInitiativeStatusWithInitiativeTypeFilterValidates() {
        var query = new IndicatorQuery(
            IndicatorCodes.COUNTRY_INITIATIVE_STATUS, null,
            List.of(new IndicatorFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("MONITORING_INITIATIVE"))),
            List.of(), null, null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
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
        var query = new IndicatorQuery(
            IndicatorCodes.FINANCIAL_INVESTMENT, null,
            List.of(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))),
            List.of(), null, null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void longTermPreservationCountAcceptsAnOptionalCountryFilter() {
        var query = new IndicatorQuery(
            IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, null,
            List.of(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("PL"))),
            List.of(), null, null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void citizenScienceNewProjectsCountAcceptsAnOptionalCountryGroupBy() {
        var query = new IndicatorQuery(
            IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, null, List.of(),
            List.of(IndicatorCodes.COUNTRY_DIMENSION), null, null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }
}
