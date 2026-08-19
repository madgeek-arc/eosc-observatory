package eu.openaire.observatory.indicator.survey;

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
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.model.ZeroDenominatorHandling;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.QuerySecurityScope;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import eu.openaire.observatory.statistics.StatsToolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No live network: the outbound WebClient call is stubbed, same pattern as
 * OpenAireStatsToolIndicatorQueryHandlerTest. Response fixtures are shaped exactly like the
 * envelope confirmed against a live deployment (see
 * SurveyRawResponse's Javadoc), with real observed values, not guessed ones.
 */
class SurveyStatsToolIndicatorQueryHandlerTest {

    private static final ResolvedFilter SURVEY_YEAR_2023 =
        new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2023"));
    private static final ResolvedFilter INITIATIVE_TYPE_OA_PUBLICATION_POLICY = new ResolvedFilter(
        IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_POLICY"));

    private final List<String> capturedRequests = new ArrayList<>();

    @Test
    void countryInitiativeStatusMapsYesNoToBooleanPerCountry() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.value()).isEqualTo(true);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(false);
        });
    }

    @Test
    void countryInitiativeStatusUsesTheQuestionCodeForTheRequestedInitiativeType() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("FINANCIAL_STRATEGY")),
            SURVEY_YEAR_2023
        );

        handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.2023.Question7\"");
    }

    @Test
    void countryInitiativeStatusSkipsCountriesWithNoAnswer() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"IT\",null]},{\"row\":[\"MT\",\"null\"]},{\"row\":[\"PT\",\"\"]}"
                + "]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
    }

    @Test
    void euCountryCoverageComputesPercentageOverRespondingCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]},{\"row\":[\"FR\",\"Yes\"]}"
                + "]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, List.of(), null, AggregationType.NONE));

        // GR=true, FR=true, DE=false -> 2/3 respondents = 66.7%, matching observatory-ui's own
        // calculatePercentage(count / result.length), not a fixed EU-membership population.
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(66.7);
        assertThat(capturedRequests).hasSize(1);
    }

    @Test
    void euCountryCoverageDenominatorIncludesNoAnswerRows() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"Yes\"]},{\"row\":[\"FR\",null]},{\"row\":[\"IT\",\"No\"]}"
                + "]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, List.of(), null, AggregationType.NONE));

        // FR's no-answer row still counts toward the denominator (matches result.length in legacy),
        // it just isn't counted as a Yes: 2/4 = 50.0%, not 2/3.
        assertThat(result.data().get(0).value()).isEqualTo(50.0);
    }

    @Test
    void countryInitiativeStatusUsesTheRequestedSurveyYearAndNeverStampsAPeriod() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY,
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2022"))
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.2022.Question6\"");
        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data().get(0).dimensions()).doesNotContainKey("period");
    }

    @Test
    void financialInvestmentUsesTheRequestedSurveyYearAndNeverStampsAPeriod() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]}]}}]}");
        var filters = List.of(new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2022")));

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.2022.Question56\"");
        assertThat(result.dimensions()).isEmpty();
        assertThat(result.data().get(0).dimensions()).doesNotContainKey("period");
        assertThat(result.data().get(0).value()).isEqualTo(26_940_000.0);
    }

    @Test
    void financialInvestmentAppliesTheMillionsScaleAndSumsByDefault() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(56_940_000.0);
    }

    @Test
    void financialInvestmentAveragesWhenAvgAggregationRequested() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.AVG));

        assertThat(result.data().get(0).value()).isEqualTo(28_470_000.0);
    }

    @Test
    void financialInvestmentTreatsAllNullFormsAsNoAnswer() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"MT\",\"null\"]},{\"row\":[\"PT\",null]},{\"row\":[\"GR\",\"\"]}"
                + "]}}]}");

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(result.data().get(0).value()).isEqualTo(26_940_000.0);
    }

    @Test
    void financialInvestmentGroupedByCountryReturnsOneRowPerCountryInEuros() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), groupBy, null, AggregationType.SUM));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("FI");
            assertThat(dp.value()).isEqualTo(26_940_000.0);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("FR");
            assertThat(dp.value()).isEqualTo(30_000_000.0);
        });
    }

    @Test
    void longTermPreservationCountUsesQuestion89AndSumsByDefault() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"PL\",\"68\"]},{\"row\":[\"RS\",\"118\"]}]}}]}");

        var result = handler.execute(plan(longTermPreservationCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.2023.Question89\"");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(186L);
    }

    @Test
    void longTermPreservationCountSkipsNoAnswerRows() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"PL\",\"68\"]},{\"row\":[\"CY\",\"0\"]},{\"row\":[\"IT\",\"\"]},{\"row\":[\"GE\",\"null\"]}"
                + "]}}]}");

        var result = handler.execute(plan(longTermPreservationCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(result.data().get(0).value()).isEqualTo(68L);
    }

    @Test
    void citizenScienceNewProjectsCountUsesQuestion101AndGroupsByCountry() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"14\"]},{\"row\":[\"IE\",\"124\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(citizenScienceNewProjectsCountDefinition(), List.of(SURVEY_YEAR_2023), groupBy, null, AggregationType.SUM));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.2023.Question101\"");
        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("FI");
            assertThat(dp.value()).isEqualTo(14L);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("IE");
            assertThat(dp.value()).isEqualTo(124L);
        });
    }

    private SurveyStatsToolIndicatorQueryHandler handlerReturning(String responseJson) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            String rawJson = request.url().getRawQuery().substring("json=".length());
            capturedRequests.add(URLDecoder.decode(rawJson, StandardCharsets.UTF_8));
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(responseJson)
                .build();
            return Mono.just(response);
        });
        StatsToolProperties properties = new StatsToolProperties()
            .setEndpoint("https://survey-stats.test/stats-api/")
            .setSurveyProfile("eosc-obs");
        SurveyStatsToolClient client = new SurveyStatsToolClient(builder, properties);
        SurveyStatsToolQueryBuilder queryBuilder = new SurveyStatsToolQueryBuilder(new ObjectMapper(), properties);
        return new SurveyStatsToolIndicatorQueryHandler(queryBuilder, client);
    }

    private static IndicatorExecutionPlan plan(IndicatorDefinition definition, List<ResolvedFilter> filters,
                                                List<ResolvedDimension> groupBy, ResolvedTimeRange timeRange,
                                                AggregationType aggregation) {
        return new IndicatorExecutionPlan(
            definition, aggregation, filters, groupBy, timeRange, null, QuerySecurityScope.unrestricted(), 1000
        );
    }

    private static TimePolicy timePolicy() {
        return new TimePolicy(
            false, MissingPeriodHandling.OMIT
        );
    }

    private static IndicatorDimensionBinding surveyYearBinding() {
        return new IndicatorDimensionBinding(
            IndicatorCodes.SURVEY_YEAR_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
        );
    }

    private static IndicatorDefinition countryInitiativeStatusDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.COUNTRY_INITIATIVE_STATUS, "Country Initiative Status", "desc", "Publications",
            IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
                ),
                surveyYearBinding()
            ),
            timePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.COUNTRY_INITIATIVE_STATUS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition euCountryCoverageDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.EU_COUNTRY_COVERAGE, "EU Country Coverage", "desc", "Publications",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
                ),
                surveyYearBinding()
            ),
            timePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.EU_COUNTRY_COVERAGE),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition financialInvestmentDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.FINANCIAL_INVESTMENT, "Financial Investment in OA Publication", "desc", "Publications",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM, Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                ),
                surveyYearBinding()
            ),
            timePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.FINANCIAL_INVESTMENT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition longTermPreservationCountDefinition() {
        return countByQuestionDefinition(IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, "Repositories with Long-Term Preservation");
    }

    private static IndicatorDefinition citizenScienceNewProjectsCountDefinition() {
        return countByQuestionDefinition(IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, "New Citizen Science Projects");
    }

    private static IndicatorDefinition countByQuestionDefinition(String code, String label) {
        return new IndicatorDefinition(
            UUID.randomUUID(), code, label, "desc", "Data Preservation",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM, Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                ),
                surveyYearBinding()
            ),
            timePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, code),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
