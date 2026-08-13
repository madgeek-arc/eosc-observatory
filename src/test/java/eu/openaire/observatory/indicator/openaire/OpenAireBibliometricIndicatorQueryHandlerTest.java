package eu.openaire.observatory.indicator.openaire;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorExecutionBinding;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.model.IndicatorStatus;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.NullHandling;
import eu.openaire.observatory.indicator.model.RatioPolicy;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.TemporalBehavior;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.model.ZeroDenominatorHandling;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.QuerySecurityScope;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No live network: the outbound WebClient call is stubbed via a mocked ExchangeFunction returning
 * a canned raw?json= response body, establishing the pattern since this repo has no existing
 * precedent (no WireMock/MockWebServer) for testing an outbound HTTP call. capturedRequests holds
 * the decoded "json" query param of every request the handler made, so tests can assert on the
 * actual DSL sent, not just the resulting value.
 */
class OpenAireStatsToolIndicatorQueryHandlerTest {

    private final List<String> capturedRequests = new ArrayList<>();

    @Test
    void accessStatusShareOaFilterCountsOnlyOpenAccess() {
        var handler = handlerReturning("{\"data\":[[[\"30\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(25.0);
        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"values\":[\"Open Access\"]");
        assertThat(numeratorQuery).doesNotContain("Closed Access").doesNotContain("Embargo").doesNotContain("Restricted");
    }

    @Test
    void accessStatusShareClosedFilterReturnsOnlyClosedStatus() {
        var handler = handlerReturning("{\"data\":[[[\"50\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("CLOSED")));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(41.7);
        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"values\":[\"Closed Access\"]");
        assertThat(numeratorQuery).doesNotContain("Embargo").doesNotContain("Restricted").doesNotContain("Open Access");
    }

    @Test
    void accessStatusShareEmbargoedFilterReturnsOnlyEmbargoedStatus() {
        var handler = handlerReturning("{\"data\":[[[\"25\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("EMBARGOED")));

        handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"values\":[\"Embargo\"]");
        assertThat(numeratorQuery).doesNotContain("Closed Access").doesNotContain("Restricted").doesNotContain("Open Access");
    }

    @Test
    void accessStatusShareRestrictedFilterReturnsOnlyRestrictedStatus() {
        var handler = handlerReturning("{\"data\":[[[\"15\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("RESTRICTED")));

        handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"values\":[\"Restricted\"]");
        assertThat(numeratorQuery).doesNotContain("Closed Access").doesNotContain("Embargo").doesNotContain("Open Access");
    }

    @Test
    void accessStatusShareInFilterAggregatesGivenStatuses() {
        var handler = handlerReturning("{\"data\":[[[\"90\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(
            IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.IN, List.of("CLOSED", "EMBARGOED", "RESTRICTED")
        ));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(75.0);
        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"op\":\"OR\"");
        assertThat(numeratorQuery).contains("Closed Access").contains("Embargo").contains("Restricted");
        assertThat(numeratorQuery).doesNotContain("\"values\":[\"Open Access\"]");
    }

    @Test
    void accessStatusShareNotEqFilterAggregatesEverythingExceptGivenStatus() {
        var handler = handlerReturning("{\"data\":[[[\"90\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.NOT_EQ, List.of("OA")));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(75.0);
        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"op\":\"OR\"");
        assertThat(numeratorQuery).contains("Closed Access").contains("Embargo").contains("Restricted");
        assertThat(numeratorQuery).doesNotContain("\"values\":[\"Open Access\"]");
    }

    @Test
    void accessStatusShareNotInFilterAggregatesEverythingExceptGivenStatuses() {
        var handler = handlerReturning("{\"data\":[[[\"90\"]],[[\"120\"]]]}");
        var filters = List.of(new ResolvedFilter(
            IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.NOT_IN, List.of("CLOSED", "EMBARGOED")
        ));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(75.0);
        String numeratorQuery = firstSubQuery(capturedRequests.get(0));
        assertThat(numeratorQuery).contains("\"op\":\"OR\"");
        assertThat(numeratorQuery).contains("Open Access").contains("Restricted");
        assertThat(numeratorQuery).doesNotContain("\"values\":[\"Closed Access\"]").doesNotContain("\"values\":[\"Embargo\"]");
    }

    @Test
    void accessStatusShareReturnsNullOnZeroDenominator() {
        var handler = handlerReturning("{\"data\":[[[\"0\"]],[[\"0\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isNull();
    }

    @Test
    void accessStatusShareOverATimeRangeIssuesOnePairPerYear() {
        var handler = handlerReturning("{\"data\":[[[\"10\"]],[[\"100\"]],[[\"20\"]],[[\"100\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")));
        var timeRange = yearRange(2021, 2022);

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), timeRange));

        assertThat(result.dimensions()).containsExactly("period");
        assertThat(result.data()).hasSize(2);
        assertThat(result.data().get(0).dimensions().get("period")).isEqualTo("2021");
        assertThat(result.data().get(0).value()).isEqualTo(10.0);
        assertThat(result.data().get(1).dimensions().get("period")).isEqualTo("2022");
        assertThat(result.data().get(1).value()).isEqualTo(20.0);
    }

    @Test
    void accessStatusShareWithCountryFilterUsesSingleCountrySubQuery() {
        var handler = handlerReturning("{\"data\":[[[\"30\"]],[[\"120\"]]]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")),
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))
        );

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(25.0);
        assertThat(capturedRequests.get(0)).contains("\"field\":\"result.organization.country.code\"").contains("\"GR\"");
    }

    @Test
    void accessStatusShareWithMultipleCountriesOrsThemTogether() {
        var handler = handlerReturning("{\"data\":[[[\"30\"]],[[\"120\"]]]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")),
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "DE"))
        );

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(25.0);
        String request = capturedRequests.get(0);
        assertThat(request).contains("\"field\":\"result.organization.country.code\"");
        assertThat(request).contains("\"GR\"").contains("\"DE\"");
        assertThat(request).contains("\"op\":\"OR\"");
    }

    @Test
    void accessStatusShareGroupedByCountryReturnsOneRowPerCountry() {
        var handler = handlerReturning(
            "{\"data\":[[[\"10\",\"GR\"],[\"5\",\"DE\"]],[[\"40\",\"GR\"],[\"50\",\"DE\"]]]}"
        );
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")));
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, groupBy, null));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.value()).isEqualTo(25.0);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(10.0);
        });
        assertThat(capturedRequests.get(0)).contains("\"field\":\"result.organization.country\"");
    }

    @Test
    void accessStatusShareGroupedByCountryTreatsCountryAbsentFromNumeratorAsZero() {
        var handler = handlerReturning(
            "{\"data\":[[[\"10\",\"GR\"]],[[\"40\",\"GR\"],[\"50\",\"DE\"]]]}"
        );
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")));
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(accessStatusShareDefinition(), filters, groupBy, null));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(0.0);
        });
    }

    @Test
    void publicationsCountFilteredBySingleAccessStatusReturnsOneCount() {
        var handler = handlerReturning("{\"data\":[[[\"4242\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.ACCESS_STATUS_DIMENSION, FilterOperator.EQ, List.of("OA")));

        var result = handler.execute(plan(countDefinition(), filters, List.of(), null));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(4242L);
    }

    @Test
    void publicationsCountGroupedByAccessStatusReturnsFourRows() {
        var handler = handlerReturning("{\"data\":[[[\"1\"]],[[\"2\"]],[[\"3\"]],[[\"4\"]]]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.ACCESS_STATUS_DIMENSION));

        var result = handler.execute(plan(countDefinition(), List.of(), groupBy, null));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.ACCESS_STATUS_DIMENSION);
        assertThat(result.data()).hasSize(4);
        assertThat(result.data()).extracting(dp -> dp.dimensions().get(IndicatorCodes.ACCESS_STATUS_DIMENSION))
            .containsExactlyInAnyOrder("OA", "CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void publicationsCountWithCountryFilterAddsCountryCodeToRequest() {
        var handler = handlerReturning("{\"data\":[[[\"777\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")));

        var result = handler.execute(plan(countDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(777L);
        assertThat(capturedRequests.get(0)).contains("\"field\":\"result.organization.country.code\"").contains("\"GR\"");
    }

    @Test
    void publicationsCountWithoutCountryFilterOmitsCountryCode() {
        var handler = handlerReturning("{\"data\":[[[\"777\"]]]}");

        handler.execute(plan(countDefinition(), List.of(), List.of(), null));

        assertThat(capturedRequests.get(0)).doesNotContain("result.organization.country.code");
    }

    @Test
    void publicationsCountWithMultipleCountriesOrsThemTogether() {
        var handler = handlerReturning("{\"data\":[[[\"999\"]]]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "DE")));

        var result = handler.execute(plan(countDefinition(), filters, List.of(), null));

        assertThat(result.data().get(0).value()).isEqualTo(999L);
        String request = capturedRequests.get(0);
        assertThat(request).contains("\"field\":\"result.organization.country.code\"");
        assertThat(request).contains("\"GR\"").contains("\"DE\"");
        assertThat(request).contains("\"op\":\"OR\"");
    }

    @Test
    void publicationsCountGroupedByCountryReturnsOneRowPerCountry() {
        var handler = handlerReturning("{\"data\":[[[\"10\",\"GR\"],[\"5\",\"DE\"]]]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(countDefinition(), List.of(), groupBy, null));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).extracting(dp -> dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION))
            .containsExactlyInAnyOrder("GR", "DE");
        assertThat(result.data()).extracting(IndicatorDataPoint::value).containsExactlyInAnyOrder(10L, 5L);
        assertThat(capturedRequests.get(0)).contains("\"field\":\"result.organization.country\"");
    }

    /**
     * A single request carries every sub-query as its own "series" entry (numerator, then
     * denominator) — isolates the first one's text so assertions about the numerator's filters
     * aren't tripped up by the denominator now deliberately mentioning all 4 access-mode literals.
     */
    private static String firstSubQuery(String request) {
        return request.substring(0, request.indexOf("}},{\"query\":"));
    }

    private OpenAireStatsToolIndicatorQueryHandler handlerReturning(String responseJson) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            String rawJson = request.url().getRawQuery().substring("json=".length());
            capturedRequests.add(URLDecoder.decode(rawJson, StandardCharsets.UTF_8));
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(responseJson)
                .build();
            return Mono.just(response);
        });
        OpenAireStatsToolClient client = new OpenAireStatsToolClient(builder, "https://openaire.test/stats-tool/");
        return new OpenAireStatsToolIndicatorQueryHandler(new OpenAireQueryBuilder(new ObjectMapper()), client);
    }

    private static ResolvedTimeRange yearRange(int fromYear, int toYear) {
        return new ResolvedTimeRange(
            TimeGrain.YEAR, LocalDate.of(fromYear, 1, 1), LocalDate.of(toYear, 1, 1), MissingPeriodHandling.ZERO_FILL
        );
    }

    private static IndicatorExecutionPlan plan(IndicatorDefinition definition, List<ResolvedFilter> filters,
                                                List<ResolvedDimension> groupBy, ResolvedTimeRange timeRange) {
        return new IndicatorExecutionPlan(
            definition, null, filters, groupBy, timeRange, QuerySecurityScope.unrestricted(), 1000
        );
    }

    private static TimePolicy timePolicy() {
        return new TimePolicy(
            true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
        );
    }

    private static IndicatorDefinition accessStatusShareDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE,
            "Publications by Access Status (Share)", "desc", "Publications",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER),
                    Set.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.NOT_EQ, FilterOperator.NOT_IN), true
                ),
                new IndicatorDimensionBinding(
                    IndicatorCodes.COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                )
            ),
            timePolicy(),
            new IndicatorExecutionBinding(
                OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE
            ),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition countDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.PUBLICATIONS_COUNT, "Publications", "desc", "Publications",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.SUM, Set.of(AggregationType.SUM), NullHandling.EXCLUDE),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ), false
                ),
                new IndicatorDimensionBinding(
                    IndicatorCodes.COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                )
            ),
            timePolicy(),
            new IndicatorExecutionBinding(OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.PUBLICATIONS_COUNT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
