package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.openaire.OpenAireBibliometricIndicatorQueryHandler;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.MissingRequiredFilterException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Confirms the catalog changes made when wiring in OpenAireBibliometricIndicatorQueryHandler:
 * publications.access_status_share (previously named publications.oa_share) had no
 * dimensionBindings at all before this work, so an accessStatus filter (the shape
 * INDICATOR_IMPLEMENTATION_PLAN.md's own first-working-example curl relies on) would have failed
 * validation. Both publications.access_status_share and publications.count route to the real
 * handler, and both accept country either as a filter (EQ or IN, one or more countries) or a
 * groupBy (for a per-country map). publications.access_status_share's accessStatus filter also
 * accepts NOT_EQ/IN/NOT_IN, not just EQ, to aggregate several statuses together.
 */
class IndicatorCatalogOpenAireWiringTest {

    private final IndicatorCatalog catalog = new IndicatorCatalog();
    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        new CatalogIndicatorDefinitionLookup(catalog), new CatalogDimensionDefinitionLookup(catalog),
        new AllowAllIndicatorAuthorizationService()
    );
    private static final ActorContext ALLOW_ALL = level -> true;

    @Test
    void accessStatusShareFilterNowValidates() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, null,
            List.of(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("CLOSED"))),
            List.of(), null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.ACCESS_STATUS_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("CLOSED");
    }

    @Test
    void accessStatusShareRequiresTheAccessStatusFilter() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, null, List.of(), List.of(), null, List.of(), null
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(MissingRequiredFilterException.class);
    }

    @Test
    void accessStatusShareAcceptsNotEqAndInOperatorsForAggregation() {
        var notEqQuery = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, null,
            List.of(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.NOT_EQ, List.of("OA"))),
            List.of(), null, List.of(), null
        );
        var inQuery = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, null,
            List.of(new IndicatorFilter(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.IN, List.of("CLOSED", "EMBARGOED", "RESTRICTED")
            )),
            List.of(), null, List.of(), null
        );

        var notEqPlan = validator.compile(notEqQuery, ALLOW_ALL);
        var inPlan = validator.compile(inQuery, ALLOW_ALL);

        assertThat(notEqPlan.filters().get(0).operator()).isEqualTo(FilterOperator.NOT_EQ);
        assertThat(inPlan.filters().get(0).values()).containsExactly("CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void accessStatusShareAcceptsNotInForAggregation() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, null,
            List.of(new IndicatorFilter(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.NOT_IN, List.of("CLOSED", "EMBARGOED", "RESTRICTED")
            )),
            List.of(), null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).operator()).isEqualTo(FilterOperator.NOT_IN);
        assertThat(plan.filters().get(0).values()).containsExactly("CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void publicationsCountAcceptsAnOptionalCountryFilter() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_COUNT, null,
            List.of(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))),
            List.of(), null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).dimensionCode()).isEqualTo(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("GR");
    }

    @Test
    void publicationsCountAcceptsAnInCountryFilterForMultipleCountries() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_COUNT, null,
            List.of(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "DE"))),
            List.of(), null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.filters().get(0).operator()).isEqualTo(FilterOperator.IN);
        assertThat(plan.filters().get(0).values()).containsExactly("GR", "DE");
    }

    @Test
    void accessStatusShareAcceptsAnOptionalCountryGroupByForAPerCountryMap() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, null,
            List.of(new IndicatorFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA"))),
            List.of(IndicatorCodes.COUNTRY_DIMENSION), null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void publicationsCountAcceptsAnOptionalCountryGroupByForAPerCountryMap() {
        var query = new IndicatorQuery(
            IndicatorCodes.PUBLICATIONS_COUNT, null, List.of(),
            List.of(IndicatorCodes.COUNTRY_DIMENSION), null, List.of(), null
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void bothPublicationIndicatorsRouteToTheRealOpenAireHandler() {
        assertThat(catalog.get(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE).executionBinding().handlerKey())
            .isEqualTo(OpenAireBibliometricIndicatorQueryHandler.HANDLER_KEY);
        assertThat(catalog.get(IndicatorCodes.PUBLICATIONS_COUNT).executionBinding().handlerKey())
            .isEqualTo(OpenAireBibliometricIndicatorQueryHandler.HANDLER_KEY);
    }
}
