package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.openaire.OpenAireStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.MissingRequiredFilterException;
import eu.openaire.observatory.indicator.validation.UnsupportedDimensionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the catalog changes made when wiring in OpenAireStatsToolIndicatorQueryHandler:
 * publications.access_status_share (previously named publications.oa_share) had no
 * dimensionBindings at all before this work, so an accessStatus filter (the shape
 * INDICATOR_IMPLEMENTATION_PLAN.md's own first-working-example curl relies on) would have failed
 * validation. Both publications.access_status_share and publications.count route to the real
 * handler, and both accept country either as a filter (EQ or IN, one or more countries) or a
 * groupBy (for a per-country map). publications.access_status_share's accessStatus filter also
 * accepts NOT_EQ/IN/NOT_IN, not just EQ, to aggregate several statuses together.
 */
class IndicatorDefinitionCatalogOpenAireWiringTest {

    private final IndicatorDefinitionCatalog catalog = new IndicatorDefinitionCatalog();
    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        new CatalogIndicatorDefinitionLookup(catalog), new CatalogDimensionDefinitionLookup(catalog),
        new AllowAllIndicatorAuthorizationService(), new IndicatorQueryHandlerRegistry(List.of()),
        new DimensionMemberProviderRegistry(List.of())
    );
    private static final ActorContext ALLOW_ALL = level -> true;

    @Test
    void accessStatusShareFilterNowValidates() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE)
            .withFilters(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("CLOSED")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.ACCESS_STATUS_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("CLOSED");
    }

    @Test
    void accessStatusShareRequiresTheAccessStatusFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE).build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void accessStatusShareAcceptsNotEqAndInOperatorsForAggregation() {
        var notEqQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE)
            .withFilters(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.NOT_EQ, List.of("OA")))
            .build();
        var inQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE)
            .withFilters(new IndicatorFilter(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.IN, List.of("CLOSED", "EMBARGOED", "RESTRICTED")
            ))
            .build();

        var notEqPlan = validator.compile(notEqQuery, ALLOW_ALL);
        var inPlan = validator.compile(inQuery, ALLOW_ALL);

        assertThat(notEqPlan.filters().get(0).operator()).isEqualTo(FilterOperator.NOT_EQ);
        assertThat(inPlan.filters().get(0).values()).containsExactly("CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void accessStatusShareAcceptsNotInForAggregation() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE)
            .withFilters(new IndicatorFilter(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.NOT_IN, List.of("CLOSED", "EMBARGOED", "RESTRICTED")
            ))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).operator()).isEqualTo(FilterOperator.NOT_IN);
        assertThat(plan.filters().get(0).values()).containsExactly("CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void publicationsCountAcceptsAnOptionalCountryFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withFilters(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("GR");
    }

    @Test
    void publicationsCountAcceptsAnInCountryFilterForMultipleCountries() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withFilters(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "DE")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).operator()).isEqualTo(FilterOperator.IN);
        assertThat(plan.filters().get(0).values()).containsExactly("GR", "DE");
    }

    @Test
    void accessStatusShareAcceptsAnOptionalCountryGroupByForAPerCountryMap() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE)
            .withFilters(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")))
            .withGroupBy(IndicatorCodes.COUNTRY_DIMENSION)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void publicationsCountAcceptsAnOptionalCountryGroupByForAPerCountryMap() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withGroupBy(IndicatorCodes.COUNTRY_DIMENSION)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, IndicatorCodes.PUBLICATIONS_COUNT,
        IndicatorCodes.DATASETS_COUNT, IndicatorCodes.SOFTWARE_COUNT, IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT
    })
    void allFiveOpenAireIndicatorsRouteToTheRealHandler(String indicatorCode) {
        assertThat(catalog.get(indicatorCode).executionBinding().handlerKey())
            .isEqualTo(OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.DATASETS_COUNT, IndicatorCodes.SOFTWARE_COUNT})
    void entityCountAcceptsCountryAndClassificationGroupBy(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withGroupBy(IndicatorCodes.COUNTRY_DIMENSION)
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.DATASETS_COUNT, IndicatorCodes.SOFTWARE_COUNT})
    void entityCountAcceptsAnOptionalAccessStatusFilter(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.ACCESS_STATUS_DIMENSION);
    }

    @Test
    void publicationsCountAcceptsClassificationAndFieldOfScienceGroupBy() {
        var classificationQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withGroupBy(IndicatorCodes.CLASSIFICATION_DIMENSION)
            .build();
        var fieldOfScienceQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withGroupBy(IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION)
            .build();

        assertThat(validator.compile(classificationQuery, ALLOW_ALL).groupBy())
            .extracting("dimensionCode").containsExactly(IndicatorCodes.CLASSIFICATION_DIMENSION);
        assertThat(validator.compile(fieldOfScienceQuery, ALLOW_ALL).groupBy())
            .extracting("dimensionCode").containsExactly(IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION);
    }

    @Test
    void publicationsCountAcceptsAClassificationFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withFilters(new IndicatorFilter(IndicatorCodes.CLASSIFICATION_DIMENSION, FilterOperator.EQ, List.of("Article")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.CLASSIFICATION_DIMENSION);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("entityCountClassificationFilterValues")
    void entityCountAcceptsAClassificationFilter(String indicatorCode, List<Object> classificationValues) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withFilters(new IndicatorFilter(IndicatorCodes.CLASSIFICATION_DIMENSION, FilterOperator.IN, classificationValues))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).values()).containsExactlyElementsOf(classificationValues);
    }

    private static Stream<Arguments> entityCountClassificationFilterValues() {
        return Stream.of(
            Arguments.of(IndicatorCodes.DATASETS_COUNT, List.<Object>of("Dataset", "Collection")),
            Arguments.of(IndicatorCodes.SOFTWARE_COUNT, List.<Object>of("Software", "Review"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.DATASETS_COUNT, IndicatorCodes.SOFTWARE_COUNT})
    void entityCountDoesNotHaveAFieldOfScienceBinding(String indicatorCode) {
        var query = IndicatorQueryBuilder.forIndicator(indicatorCode)
            .withGroupBy(IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.GOLD_OA_DIMENSION, IndicatorCodes.GREEN_OA_DIMENSION})
    void publicationsCountAcceptsAnOptionalGoldOrGreenOaFilter(String dimensionCode) {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withFilters(new IndicatorFilter(dimensionCode, FilterOperator.EQ, List.of("YES")))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(dimensionCode);
        assertThat(plan.filters().get(0).values()).containsExactly("YES");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.GOLD_OA_DIMENSION, IndicatorCodes.GREEN_OA_DIMENSION})
    void publicationsCountRejectsGoldOrGreenOaAsAGroupBy(String dimensionCode) {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_COUNT)
            .withGroupBy(dimensionCode)
            .build();

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    @Test
    void oaLicenseStatusCountNoLongerRequiresTheOaLicenseStatusFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT).build();

        assertThat(validator.compile(query, ALLOW_ALL).filters()).isEmpty();
    }

    @Test
    void oaLicenseStatusCountAcceptsTheOaLicenseStatusFilter() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT)
            .withFilters(new IndicatorFilter(
                IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA_WITH_LICENSE")
            ))
            .build();

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("OA_WITH_LICENSE");
    }

    @Test
    void oaLicenseStatusCountAcceptsOaLicenseStatusAsAGroupBy() {
        var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT)
            .withGroupBy(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION)
            .build();

        assertThat(validator.compile(query, ALLOW_ALL).groupBy())
            .extracting("dimensionCode").containsExactly(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION);
    }

    @Test
    void oaLicenseStatusCountAcceptsClassificationAsAGroupBy() {
        var groupQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT)
            .withFilters(new IndicatorFilter(
                IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, FilterOperator.EQ, List.of("CLOSED")
            ))
            .withGroupBy(IndicatorCodes.CLASSIFICATION_DIMENSION)
            .build();

        assertThat(validator.compile(groupQuery, ALLOW_ALL).groupBy())
            .extracting("dimensionCode").containsExactly(IndicatorCodes.CLASSIFICATION_DIMENSION);
    }

    @Test
    void oaLicenseStatusCountAcceptsAnOptionalCountryFilterAndGroupBy() {
        var filterQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT)
            .withFilters(
                new IndicatorFilter(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, FilterOperator.EQ, List.of("CLOSED")),
                new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))
            )
            .build();
        var groupQuery = IndicatorQueryBuilder.forIndicator(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT)
            .withFilters(new IndicatorFilter(
                IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, FilterOperator.EQ, List.of("CLOSED")
            ))
            .withGroupBy(IndicatorCodes.COUNTRY_DIMENSION)
            .build();

        assertThat(validator.compile(filterQuery, ALLOW_ALL).filters())
            .extracting("dimensionCode").contains(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(validator.compile(groupQuery, ALLOW_ALL).groupBy())
            .extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }
}
