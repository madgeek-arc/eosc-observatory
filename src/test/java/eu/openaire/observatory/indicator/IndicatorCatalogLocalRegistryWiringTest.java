package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.local.LocalRegistryIndicatorQueryHandler;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.MissingRequiredFilterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the catalog wiring for the 2 local-registry-backed indicators (no external HTTP call):
 * survey.countries_with_validated_answers requires a surveyId filter (there is no meaningful
 * value without picking which survey), and stakeholder.participating_countries_count needs no
 * filters at all (its stakeholderType is hardcoded in the handler, not a client-supplied
 * dimension). Both route to LocalRegistryIndicatorQueryHandler, not the OpenAIRE or survey
 * stats-tool handlers.
 */
class IndicatorCatalogLocalRegistryWiringTest {

    private final IndicatorCatalog catalog = new IndicatorCatalog();
    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        new CatalogIndicatorDefinitionLookup(catalog), new CatalogDimensionDefinitionLookup(catalog),
        new AllowAllIndicatorAuthorizationService(), new IndicatorQueryHandlerRegistry(List.of())
    );
    private static final ActorContext ALLOW_ALL = level -> true;

    @Test
    void countriesWithValidatedAnswersRequiresTheSurveyIdFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS).build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void countriesWithValidatedAnswersWithSurveyIdFilterValidates() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS)
            .withFilters(new IndicatorFilter(IndicatorCodes.SURVEY_ID_DIMENSION, FilterOperator.EQ, List.of("m-eosc-sb-2023")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.SURVEY_ID_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("m-eosc-sb-2023");
    }

    @Test
    void stakeholderParticipatingCountriesCountNeedsNoFilters() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT).build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).isEmpty();
    }

    @Test
    void bothLocalRegistryIndicatorsRouteToTheLocalRegistryHandler() {
        assertThat(catalog.get(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS).executionBinding().handlerKey())
            .isEqualTo(LocalRegistryIndicatorQueryHandler.HANDLER_KEY);
        assertThat(catalog.get(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT).executionBinding().handlerKey())
            .isEqualTo(LocalRegistryIndicatorQueryHandler.HANDLER_KEY);
    }
}
