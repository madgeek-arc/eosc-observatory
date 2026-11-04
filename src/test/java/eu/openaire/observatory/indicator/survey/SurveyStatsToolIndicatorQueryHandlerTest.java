package eu.openaire.observatory.indicator.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.indicator.CatalogDimensionDefinitionLookup;
import eu.openaire.observatory.service.SurveyService;
import eu.openaire.observatory.indicator.ComparisonCalculator;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.IndicatorQueryService;
import eu.openaire.observatory.indicator.TimeSeriesAggregationCalculator;
import eu.openaire.observatory.indicator.TimeSeriesNormalizer;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorExecutionBinding;
import eu.openaire.observatory.indicator.model.IndicatorFormat;
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
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQueryBuilder;
import eu.openaire.observatory.indicator.result.IndicatorExclusion;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.QuerySecurityScope;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.indicator.validation.ResolvedTimeRange;
import eu.openaire.observatory.statistics.StatsToolProperties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    private static final ResolvedFilter INVESTMENT_AREA_OA_PUBLICATION = new ResolvedFilter(
        IndicatorCodes.INVESTMENT_AREA_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION"));

    private final List<String> capturedRequests = new ArrayList<>();

    @Test
    void serviceNormalizesYearAndDeduplicatesCountriesBeforeNumericAggregation() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"100.5\"]},{\"row\":[\"DE\",\"200.25\"]}]}}]}");
        var catalog = new IndicatorDefinitionCatalog();
        var providers = new DimensionMemberProviderRegistry(List.of());
        var handlers = new IndicatorQueryHandlerRegistry(List.of(handler));
        var validator = new IndicatorQueryValidator(
            code -> financialInvestmentDefinition(), new CatalogDimensionDefinitionLookup(catalog),
            new AllowAllIndicatorAuthorizationService(), handlers, providers);
        var service = new IndicatorQueryService(validator, handlers,
            new TimeSeriesNormalizer(providers),
            new TimeSeriesAggregationCalculator(),
            new ComparisonCalculator());
        for (List<Object> countries : List.of(List.<Object>of("GR", "DE"), List.<Object>of("GR", "GR", "DE"))) {
            var query = IndicatorQueryBuilder.forIndicator(IndicatorCodes.FINANCIAL_INVESTMENT)
                .withFilters(
                    new IndicatorFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of(" 2023 ")),
                    new IndicatorFilter(IndicatorCodes.INVESTMENT_AREA_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION")),
                    new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, countries))
                .build();
            assertThat(service.execute(query, level -> true).data()).singleElement()
                .satisfies(point -> assertThat(point.value()).isEqualTo(300_750_000.0));
        }
        assertThat(capturedRequests).isNotEmpty().allSatisfy(request -> assertThat(request).contains("\"year\":2024"));
    }

    @Test
    void countryInitiativeStatusMapsYesNoToStatusPerCountry() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_YES);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_NO);
        });
    }

    @Test
    void countryInitiativeStatusUsesTheQuestionCodeForTheRequestedInitiativeType() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_FINANCIAL_STRATEGY")),
            SURVEY_YEAR_2023
        );

        handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question7\"", "\"year\":2024");
    }

    @Test
    void countryInitiativeStatusReportsNoAnswerCountriesAsAwaitingData() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"IT\",null]},{\"row\":[\"MT\",\"null\"]},{\"row\":[\"PT\",\"\"]}"
                + "]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(4);
        assertThat(result.data())
            .filteredOn(dp -> dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION).equals("GR"))
            .extracting("value")
            .containsExactly(SurveyStatsToolIndicatorQueryHandler.STATUS_YES);
        assertThat(result.data())
            .filteredOn(dp -> List.of("IT", "MT", "PT").contains(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)))
            .extracting("value")
            .containsOnly(SurveyStatsToolIndicatorQueryHandler.STATUS_AWAITING_DATA);
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
    void countryInitiativeStatusNarrowsToRequestedCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]},{\"row\":[\"FR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
    }

    @Test
    void countryInitiativeStatusIncludesARequestedCountryWithNoDataAsAwaitingData() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "XX"))
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_YES);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("XX");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_AWAITING_DATA);
        });
    }

    // --- Fan-out bounds ---

    @Test
    void yearsBeforeTheFirstSurveyAreAnsweredWithoutCallingTheStatsTool() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var timeRange = new ResolvedTimeRange(2015, 2023, MissingPeriodHandling.NULL_FILL);

        handler.execute(plan(countryInitiativeStatusDefinition(), List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY),
            List.of(), timeRange, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(3)
            .allSatisfy(request -> assertThat(request).doesNotContain("\"year\":2021", "\"year\":2020", "\"year\":2015"));
    }

    @Test
    void aQueryNeedingMoreCallsThanAllowedIsRejectedBeforeAnyCall() {
        var handler = handlerReturning("{\"datasets\":[]}");
        var allTypes = new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN,
            List.<Object>copyOf(eu.openaire.observatory.indicator.dimension.OpenScienceArea.initiativeQuestionCodes().keySet()));
        var timeRange = new ResolvedTimeRange(2021, 2026, MissingPeriodHandling.NULL_FILL);

        assertThatThrownBy(() -> handler.execute(plan(countryInitiativeStatusDefinition(), List.of(allTypes),
            List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)), timeRange, AggregationType.NONE)))
            .isInstanceOf(eu.openaire.observatory.indicator.validation.InvalidFilterShapeException.class)
            .hasMessageContaining("222");
        assertThat(capturedRequests).isEmpty();
    }

    @Test
    void theTwelveAreaPolicyMatrixOverTheSurveyYearsStaysWithinTheBudget() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"No\"]}]}}]}");
        var policyTypes = new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN,
            List.<Object>copyOf(eu.openaire.observatory.indicator.dimension.OpenScienceArea.initiativeTypes(
                eu.openaire.observatory.indicator.dimension.OpenScienceArea.InitiativeKind.POLICY)));
        var timeRange = new ResolvedTimeRange(2015, 2026, MissingPeriodHandling.NULL_FILL);

        handler.execute(plan(countryPolicyStatusDefinition(), List.of(policyTypes),
            List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)), timeRange, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(12 * 4 * 2);
    }

    @Test
    void countryPolicyStatusRejectsNonPolicyInitiativeTypesWithoutQueryingTheSurvey() {
        var handler = handlerReturning("{\"datasets\":[]}");
        for (String type : List.of("OPEN_DATA_FINANCIAL_STRATEGY", "OPEN_DATA_MONITORING_INITIATIVE")) {
            var filters = List.of(
                new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of(type)), SURVEY_YEAR_2023);

            org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                    handler.execute(plan(countryPolicyStatusDefinition(), filters, List.of(), null, AggregationType.NONE)))
                .isInstanceOf(eu.openaire.observatory.indicator.validation.InvalidFilterShapeException.class)
                .hasMessageContaining(type);
        }
        assertThat(capturedRequests).isEmpty();
    }

    @Test
    void countryPolicyStatusCombinesImmediateOaPolicyWithItsMandatorySubQuestion() {
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}",
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"No\"]}]}}]}"
        ));
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("IMMEDIATE_OA_POLICY")),
            SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryPolicyStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question6.3\"", "\"year\":2024");
        assertThat(capturedRequests.get(1)).contains("\"name\":\"eosc.sb.Question6.3.1\"", "\"year\":2024");
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_NON_MANDATORY);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_NO_POLICY);
        });
    }

    @Test
    void countryPolicyStatusNarrowsToRequestedCountries() {
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}",
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}"
        ));
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))
        );

        var result = handler.execute(plan(countryPolicyStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
    }

    @Test
    void countryPolicyStatusIncludesARequestedCountryWithNoDataAsAwaitingData() {
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}",
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}"
        ));
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "XX"))
        );

        var result = handler.execute(plan(countryPolicyStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("XX");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_AWAITING_DATA);
        });
    }

    @Test
    void monitoredAreasCountNarrowsToRequestedCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var filters = List.of(
            SURVEY_YEAR_2023, new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR"))
        );

        var result = handler.execute(plan(monitoredAreasCountDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
    }

    @Test
    void monitoredAreasCountIncludesARequestedCountryWithNoDataAsNull() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var filters = List.of(
            SURVEY_YEAR_2023, new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "XX"))
        );

        var result = handler.execute(plan(monitoredAreasCountDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("XX");
            assertThat(dp.value()).isNull();
        });
    }

    @Test
    void euCountryCoverageNarrowsPercentageToRequestedCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]},{\"row\":[\"FR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "DE"))
        );

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, List.of(), null, AggregationType.NONE));

        // Scoped to GR (Yes) + DE (No) only: 1/2 = 50.0%, not 2/3 over all three respondents.
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(50.0);
    }

    @Test
    void euCountryCoverageGroupedByCountryReturnsOnePercentRowPerCountry() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, groupBy, null, AggregationType.NONE));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(valueFor(result, "GR")).isEqualTo(100.0);
        assertThat(valueFor(result, "DE")).isEqualTo(0.0);
    }

    @Test
    void euCountryCoverageGroupedByCountryIncludesARequestedCountryWithNoDataAsNull() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("GR", "XX"))
        );
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, groupBy, null, AggregationType.NONE));

        assertThat(result.data()).hasSize(2);
        assertThat(valueFor(result, "GR")).isEqualTo(100.0);
        assertThat(valueFor(result, "XX")).isNull();
    }

    @Test
    void euCountryCoverageGroupedByCountryReportsAwaitingDataAsNull() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"FR\",null]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, groupBy, null, AggregationType.NONE));

        assertThat(result.data()).hasSize(2);
        assertThat(valueFor(result, "GR")).isEqualTo(100.0);
        assertThat(valueFor(result, "FR")).isNull();
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

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question6\"", "\"year\":2023");
        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(result.data().get(0).dimensions()).doesNotContainKey("period");
    }

    @Test
    void financialInvestmentUsesTheRequestedSurveyYearAndNeverStampsAPeriod() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]}]}}]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2022")),
            INVESTMENT_AREA_OA_PUBLICATION
        );

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question56\"", "\"year\":2023");
        assertThat(result.dimensions()).isEmpty();
        assertThat(result.data().get(0).dimensions()).doesNotContainKey("period");
        assertThat(result.data().get(0).value()).isEqualTo(26_940_000.0);
    }

    @Test
    void financialInvestmentAppliesTheMillionsScaleAndSumsByDefault() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION), List.of(), null, AggregationType.SUM));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(56_940_000.0);
    }

    @Test
    void financialInvestmentAveragesWhenAvgAggregationRequested() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION), List.of(), null, AggregationType.AVG));

        assertThat(result.data().get(0).value()).isEqualTo(28_470_000.0);
    }

    @Test
    void financialInvestmentTreatsAllNullFormsAsNoAnswer() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"MT\",\"null\"]},{\"row\":[\"PT\",null]},{\"row\":[\"GR\",\"\"]}"
                + "]}}]}");

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION), List.of(), null, AggregationType.SUM));

        assertThat(result.data().get(0).value()).isEqualTo(26_940_000.0);
    }

    @Test
    void financialInvestmentGroupedByCountryReturnsOneRowPerCountryInEuros() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION), groupBy, null, AggregationType.SUM));

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
    void financialInvestmentGroupedByCountryNarrowsToRequestedCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));
        var filters = List.of(
            SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("FI"))
        );

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, groupBy, null, AggregationType.SUM));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("FI");
        assertThat(result.data().get(0).value()).isEqualTo(26_940_000.0);
    }

    @Test
    void financialInvestmentGroupedByAreaAndCountryNarrowsToRequestedCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]},{\"row\":[\"FR\",\"30\"]}]}}]}");
        var groupBy = List.of(
            new ResolvedDimension(IndicatorCodes.INVESTMENT_AREA_DIMENSION), new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));
        var filters = List.of(
            SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("FI"))
        );

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, groupBy, null, AggregationType.SUM));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("FI");
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.INVESTMENT_AREA_DIMENSION)).isEqualTo("OA_PUBLICATION");
    }

    @Test
    void financialInvestmentGroupedByCountryIncludesARequestedCountryWithNoDataAsNull() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"26.94\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));
        var filters = List.of(
            SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION,
            new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("FI", "XX"))
        );

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, groupBy, null, AggregationType.SUM));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("XX");
            assertThat(dp.value()).isNull();
        });
    }

    @Test
    void longTermPreservationCountUsesQuestion89AndSumsByDefault() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"PL\",\"68\"]},{\"row\":[\"RS\",\"118\"]}]}}]}");

        var result = handler.execute(plan(longTermPreservationCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question89\"", "\"year\":2024");
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

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question101\"", "\"year\":2024");
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

    @Test
    void countByQuestionGroupedByCountryNarrowsToRequestedCountries() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"PL\",\"68\"]},{\"row\":[\"RS\",\"118\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));
        var filters = List.of(
            SURVEY_YEAR_2023, new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.EQ, List.of("PL"))
        );

        var result = handler.execute(plan(longTermPreservationCountDefinition(), filters, groupBy, null, AggregationType.SUM));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("PL");
        assertThat(result.data().get(0).value()).isEqualTo(68L);
    }

    @Test
    void countByQuestionGroupedByCountryIncludesARequestedCountryWithNoDataAsNull() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"PL\",\"68\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));
        var filters = List.of(
            SURVEY_YEAR_2023, new ResolvedFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("PL", "XX"))
        );

        var result = handler.execute(plan(longTermPreservationCountDefinition(), filters, groupBy, null, AggregationType.SUM));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("XX");
            assertThat(dp.value()).isNull();
        });
    }

    @Test
    void financialInvestmentSkipsAreasWhoseQuestionIsPausedForThatYear() {
        var handler = handlerWithPausedQuestions(year -> year == 2023 ? Set.of("Question56", "Question76") : Set.of(),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests).hasSize(10)
            .noneMatch(r -> r.contains("Question56\"") || r.contains("Question76\""));
        assertThat(result.data().get(0).value()).isEqualTo(100_000_000.0);
    }

    @Test
    void financialInvestmentCountsAPausedQuestionInAYearWhereItIsNotPaused() {
        var handler = handlerWithPausedQuestions(year -> year == 2023 ? Set.of("Question56") : Set.of(),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2022")),
            INVESTMENT_AREA_OA_PUBLICATION);

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(10_000_000.0);
    }

    @Test
    void monitoredAreasCountReportsTheAreasCountedAsTheTotalAndTheOnesLeftOutAsExclusions() {
        var handler = handlerWithPausedQuestions(year -> year == 2023 ? Set.of("Question74", "Question78") : Set.of(),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");

        var paused = handler.execute(plan(monitoredAreasCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.NONE));
        var unpaused = handler.execute(plan(monitoredAreasCountDefinition(),
            List.of(new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2022"))),
            List.of(), null, AggregationType.NONE));

        assertThat(paused.data()).singleElement().satisfies(dp -> {
            assertThat(dp.value()).isEqualTo(10L);
            assertThat(dp.total()).isEqualTo(10L);
        });
        assertThat(paused.exclusions()).containsExactlyInAnyOrder(
            IndicatorExclusion.questionPaused(2023, IndicatorCodes.INITIATIVE_TYPE_DIMENSION, "SERVICES_MONITORING_INITIATIVE"),
            IndicatorExclusion.questionPaused(2023, IndicatorCodes.INITIATIVE_TYPE_DIMENSION, "REPOSITORIES_CONNECT_MONITORING_INITIATIVE"));
        assertThat(unpaused.data()).singleElement().satisfies(dp -> {
            assertThat(dp.value()).isEqualTo(12L);
            assertThat(dp.total()).isEqualTo(12L);
        });
        assertThat(unpaused.exclusions()).isEmpty();
    }

    @Test
    void financialInvestmentReportsThePausedAreasItLeftOut() {
        var handler = handlerWithPausedQuestions(year -> Set.of("Question76", "Question80"),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(result.exclusions()).containsExactlyInAnyOrder(
            IndicatorExclusion.questionPaused(2023, IndicatorCodes.INVESTMENT_AREA_DIMENSION, "SERVICES"),
            IndicatorExclusion.questionPaused(2023, IndicatorCodes.INVESTMENT_AREA_DIMENSION, "REPOSITORIES_CONNECT"));
    }

    @Test
    void countByQuestionReportsAWholeValueExclusionForAPausedQuestion() {
        var handler = handlerWithPausedQuestions(year -> Set.of("Question89"),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");

        var result = handler.execute(plan(longTermPreservationCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests).isEmpty();
        assertThat(result.exclusions()).containsExactly(IndicatorExclusion.questionPaused(2023));
        assertThat(result.data().get(0).value()).isNull();
    }

    @Test
    void initiativeIndicatorsReportThePausedMember() {
        var handler = handlerWithPausedQuestions(year -> Set.of("Question26"),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var servicesPolicy = new ResolvedFilter(
            IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("SERVICES_POLICY"));
        var filters = List.of(SURVEY_YEAR_2023, servicesPolicy);
        var expected = IndicatorExclusion.questionPaused(2023, IndicatorCodes.INITIATIVE_TYPE_DIMENSION, "SERVICES_POLICY");

        var coverage = handler.execute(plan(euCountryCoverageDefinition(), filters, List.of(), null, AggregationType.NONE));
        var status = handler.execute(plan(countryPolicyStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(capturedRequests).isEmpty();
        assertThat(coverage.exclusions()).containsExactly(expected);
        assertThat(status.exclusions()).containsExactly(expected);
    }

    @Test
    void aSubQuestionOfAPausedQuestionIsPausedToo() {
        PausedSurveyQuestions paused = new PausedSurveyQuestions(year -> Set.of("Question30"));

        assertThat(paused.isPaused(2025, "Question30.1")).isTrue();
        assertThat(paused.isPaused(2025, "Question300")).isFalse();
    }

    private SurveyStatsToolIndicatorQueryHandler handlerWithPausedQuestions(
            java.util.function.IntFunction<Set<String>> paused, String responseJson) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            capturedRequests.add(URLDecoder.decode(
                request.url().getRawQuery().substring("json=".length()), StandardCharsets.UTF_8));
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(responseJson).build());
        });
        StatsToolProperties properties = new StatsToolProperties()
            .setEndpoint("https://survey-stats.test/stats-api/")
            .setSurveyProfile("eosc-obs");
        return new SurveyStatsToolIndicatorQueryHandler(
            new SurveyStatsToolQueryBuilder(new ObjectMapper(), properties),
            new SurveyStatsToolClient(builder, properties),
            new PausedSurveyQuestions(paused));
    }

    private SurveyStatsToolIndicatorQueryHandler handlerReturning(String responseJson) {
        return handlerReturningEachInTurn(List.of(responseJson));
    }

    /** Returns HTTP 422 (the live tool's "no such named query" answer) for any request with that stats-tool {@code year}, and 200 + {@code okBody} for every other year. */
    private SurveyStatsToolIndicatorQueryHandler handlerReturning422ForYear(int unavailableYear, String okBody) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            String rawJson = URLDecoder.decode(
                request.url().getRawQuery().substring("json=".length()), StandardCharsets.UTF_8);
            capturedRequests.add(rawJson);
            ClientResponse response = rawJson.contains("\"year\":" + unavailableYear)
                ? ClientResponse.create(HttpStatus.UNPROCESSABLE_ENTITY).body("").build()
                : ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(okBody).build();
            return Mono.just(response);
        });
        StatsToolProperties properties = new StatsToolProperties()
            .setEndpoint("https://survey-stats.test/stats-api/")
            .setSurveyProfile("eosc-obs");
        SurveyStatsToolClient client = new SurveyStatsToolClient(builder, properties);
        SurveyStatsToolQueryBuilder queryBuilder = new SurveyStatsToolQueryBuilder(new ObjectMapper(), properties);
        return new SurveyStatsToolIndicatorQueryHandler(queryBuilder, client, PausedSurveyQuestions.none());
    }

    /** Each call to the stubbed client returns the next response in order — for tests needing distinct answers per fetch (e.g. countryPolicyStatus's base + mandatory sub-question). The last response repeats once the list is exhausted. */
    private SurveyStatsToolIndicatorQueryHandler handlerReturningEachInTurn(List<String> responseJsonInOrder) {
        List<String> queue = new ArrayList<>(responseJsonInOrder);
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            String rawJson = request.url().getRawQuery().substring("json=".length());
            capturedRequests.add(URLDecoder.decode(rawJson, StandardCharsets.UTF_8));
            String body = queue.size() > 1 ? queue.remove(0) : queue.get(0);
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
            return Mono.just(response);
        });
        StatsToolProperties properties = new StatsToolProperties()
            .setEndpoint("https://survey-stats.test/stats-api/")
            .setSurveyProfile("eosc-obs");
        SurveyStatsToolClient client = new SurveyStatsToolClient(builder, properties);
        SurveyStatsToolQueryBuilder queryBuilder = new SurveyStatsToolQueryBuilder(new ObjectMapper(), properties);
        return new SurveyStatsToolIndicatorQueryHandler(queryBuilder, client, PausedSurveyQuestions.none());
    }

    private static IndicatorExecutionPlan plan(IndicatorDefinition definition, List<ResolvedFilter> filters,
                                                List<ResolvedDimension> groupBy, ResolvedTimeRange timeRange,
                                                AggregationType aggregation) {
        return new IndicatorExecutionPlan(
            definition, aggregation, filters, groupBy, timeRange, null, QuerySecurityScope.unrestricted(), 1000, List.of()
        );
    }

    /** Every survey-stats-tool indicator is trend-capable — a timeSeries range yields one point per year, NULL_FILL for a year with no upstream data. */
    private static TimePolicy trendTimePolicy() {
        return new TimePolicy(true, MissingPeriodHandling.NULL_FILL);
    }

    /** surveyYear is filter-only and optional on every indicator here, since a timeSeries range is the alternative way to pick years. */
    private static IndicatorDimensionBinding optionalSurveyYearBinding() {
        return new IndicatorDimensionBinding(
            IndicatorCodes.SURVEY_YEAR_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), false
        );
    }

    private static IndicatorDimensionBinding initiativeTypeFilterGroupBinding() {
        return new IndicatorDimensionBinding(
            IndicatorCodes.INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
            Set.of(FilterOperator.EQ, FilterOperator.IN), true
        );
    }

    private static IndicatorDefinition countryInitiativeStatusDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.COUNTRY_INITIATIVE_STATUS, "Country Initiative Status", "desc",
            IndicatorValueType.TEXT, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, IndicatorFormat.YES_NO, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(initiativeTypeFilterGroupBinding(), optionalSurveyYearBinding()),
            trendTimePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.COUNTRY_INITIATIVE_STATUS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition euCountryCoverageDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.EU_COUNTRY_COVERAGE, "EU Country Coverage", "desc",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, IndicatorFormat.PERCENTAGE, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(initiativeTypeFilterGroupBinding(), optionalSurveyYearBinding()),
            trendTimePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.EU_COUNTRY_COVERAGE),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition countryPolicyStatusDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.COUNTRY_POLICY_STATUS, "Country Policy Status", "desc",
            IndicatorValueType.TEXT, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, IndicatorFormat.YES_NO, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(initiativeTypeFilterGroupBinding(), optionalSurveyYearBinding()),
            trendTimePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.COUNTRY_POLICY_STATUS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition monitoredAreasCountDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.MONITORED_AREAS_COUNT, "Monitored Areas Count", "desc",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(optionalSurveyYearBinding()),
            trendTimePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.MONITORED_AREAS_COUNT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition policyAreasCountDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.POLICY_AREAS_COUNT, "Policy Areas Count", "desc",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(optionalSurveyYearBinding()),
            trendTimePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.POLICY_AREAS_COUNT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    private static IndicatorDefinition softwareOpenSourceSetsCountDefinition() {
        return countByQuestionDefinition(IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT, "Open-Source Software Sets Published");
    }

    private static IndicatorDefinition financialInvestmentDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.FINANCIAL_INVESTMENT, "Financial Investment", "desc",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, IndicatorFormat.CURRENCY, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MEDIAN, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                ),
                optionalSurveyYearBinding(),
                new IndicatorDimensionBinding(
                    IndicatorCodes.INVESTMENT_AREA_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                )
            ),
            trendTimePolicy(),
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
            UUID.randomUUID(), code, label, "desc",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
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
                optionalSurveyYearBinding()
            ),
            trendTimePolicy(),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, code),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    @Test
    void financialInvestmentWithTimeSeriesReturnsOnePointPerYear() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(INVESTMENT_AREA_OA_PUBLICATION), List.of(), timeRange, AggregationType.SUM));

        assertThat(capturedRequests).hasSize(2);
        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question56\"", "\"year\":2023");
        assertThat(capturedRequests.get(1)).contains("\"name\":\"eosc.sb.Question56\"", "\"year\":2024");
        assertThat(result.dimensions()).containsExactly("period");
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).allSatisfy(dp -> assertThat(dp.value()).isEqualTo(10_000_000.0));
        assertThat(result.data()).extracting(dp -> dp.dimensions().get("period"))
            .containsExactlyInAnyOrder("2022", "2023");
    }

    @Test
    void countByQuestionWithTimeSeriesAndCountryGroupByReturnsOnePointPerYearPerCountry() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"PL\",\"68\"]}]}}]}");
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));

        var result = handler.execute(
            plan(longTermPreservationCountDefinition(), List.of(), groupBy, timeRange, AggregationType.SUM));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION, "period");
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).allSatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("PL");
            assertThat(dp.value()).isEqualTo(68L);
        });
        assertThat(result.data()).extracting(dp -> dp.dimensions().get("period"))
            .containsExactlyInAnyOrder("2022", "2023");
    }

    @Test
    void financialInvestmentMediansWhenMedianAggregationRequested() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]},{\"row\":[\"FR\",\"20\"]},{\"row\":[\"GR\",\"30\"]}]}}]}");

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION), List.of(), null, AggregationType.MEDIAN));

        assertThat(result.data().get(0).value()).isEqualTo(20_000_000.0);
    }

    @Test
    void financialInvestmentWithoutInvestmentAreaSumsEveryArea() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");

        var result = handler.execute(plan(financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        // 12 investmentArea members, each contributing FI=10M from the stubbed response -> 120M summed.
        assertThat(capturedRequests).hasSize(12);
        assertThat(result.dimensions()).isEmpty();
        assertThat(result.data().get(0).value()).isEqualTo(120_000_000.0);
    }

    @Test
    void financialInvestmentGroupedByInvestmentAreaReturnsOnePointPerArea() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.INVESTMENT_AREA_DIMENSION));
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.INVESTMENT_AREA_DIMENSION, FilterOperator.IN, List.of("OA_PUBLICATION", "OPEN_DATA")),
            SURVEY_YEAR_2023
        );

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, groupBy, null, AggregationType.SUM));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.INVESTMENT_AREA_DIMENSION);
        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).allSatisfy(dp -> assertThat(dp.value()).isEqualTo(10_000_000.0));
        assertThat(result.data()).extracting(dp -> dp.dimensions().get(IndicatorCodes.INVESTMENT_AREA_DIMENSION))
            .containsExactlyInAnyOrder("OA_PUBLICATION", "OPEN_DATA");
    }

    @Test
    void countryInitiativeStatusGroupedByInitiativeTypeReturnsOneRowPerCountryPerType() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION));
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN, List.of("OA_PUBLICATION_POLICY", "OPEN_DATA_POLICY")),
            SURVEY_YEAR_2023
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, groupBy, null, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(2);
        assertThat(result.dimensions()).containsExactlyInAnyOrder(IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(result.data()).hasSize(4);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.dimensions().get(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)).isEqualTo("OPEN_DATA_POLICY");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_YES);
        });
    }

    @Test
    void euCountryCoverageGroupedByInitiativeTypeAndTrendedReturnsOnePercentPerYearPerType() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION));
        var filters = List.of(new ResolvedFilter(
            IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN, List.of("OA_PUBLICATION_POLICY", "OPEN_DATA_POLICY")));
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(euCountryCoverageDefinition(), filters, groupBy, timeRange, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(4);
        assertThat(result.dimensions()).containsExactlyInAnyOrder("period", IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(result.data()).hasSize(4);
        assertThat(result.data()).allSatisfy(dp -> assertThat(dp.value()).isEqualTo(50.0));
    }

    @Test
    void countryPolicyStatusCombinesBaseAndMandatoryAnswers() {
        // GR: Yes+Yes -> MANDATORY. DE: Yes+No -> NON_MANDATORY. FR: No (mandatory not asked) -> NO_POLICY. IT: no answer -> AWAITING_DATA.
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"Yes\"]},{\"row\":[\"FR\",\"No\"]},{\"row\":[\"IT\",null]}"
                + "]}}]}",
            "{\"datasets\":[{\"series\":{\"result\":["
                + "{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}"
                + "]}}]}"
        ));
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryPolicyStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(2);
        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question6\"", "\"year\":2024");
        assertThat(capturedRequests.get(1)).contains("\"name\":\"eosc.sb.Question6.1\"", "\"year\":2024");
        assertThat(result.data()).hasSize(4);
        assertThat(valueFor(result, "GR")).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_MANDATORY);
        assertThat(valueFor(result, "DE")).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_NON_MANDATORY);
        assertThat(valueFor(result, "FR")).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_NO_POLICY);
        assertThat(valueFor(result, "IT")).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_AWAITING_DATA);
    }

    @Test
    void countryPolicyStatusGroupedByInitiativeTypeReturnsOneStatusPerCountryPerArea() {
        // Same rows for every fetch: base GR=Yes/DE=No, mandatory GR=Yes -> GR MANDATORY, DE NO_POLICY, per type.
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION));
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN,
                List.of("OA_PUBLICATION_POLICY", "OPEN_DATA_POLICY")),
            SURVEY_YEAR_2023
        );

        var result = handler.execute(plan(countryPolicyStatusDefinition(), filters, groupBy, null, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(4); // base + mandatory sub-question, per type
        assertThat(result.dimensions()).containsExactlyInAnyOrder(
            IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(result.data()).hasSize(4);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
            assertThat(dp.dimensions().get(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)).isEqualTo("OPEN_DATA_POLICY");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_MANDATORY);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("DE");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_NO_POLICY);
        });
    }

    private static Object valueFor(IndicatorResult result, String country) {
        return result.data().stream()
            .filter(dp -> dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION).equals(country))
            .findFirst().orElseThrow().value();
    }

    @Test
    void monitoredAreasCountSumsYesAnswersAcrossEveryMonitoringMember() {
        // Every request returns the same GR=Yes/DE=No pair regardless of which monitoring question was asked.
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");

        var result = handler.execute(plan(monitoredAreasCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.NONE));

        // 12 areas each have a "*_MONITORING_INITIATIVE" member -> GR answers Yes to all 12, DE to none.
        assertThat(capturedRequests).hasSize(12);
        assertThat(valueFor(result, "GR")).isEqualTo(12L);
        assertThat(valueFor(result, "DE")).isEqualTo(0L);
    }

    @Test
    void monitoredAreasCountGroupedByInitiativeTypeCountsYesCountriesPerArea() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]},{\"row\":[\"FR\",\"Yes\"]}]}}]}");

        var result = handler.execute(plan(monitoredAreasCountDefinition(), List.of(SURVEY_YEAR_2023),
            List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(12).allSatisfy(dp -> {
            assertThat(dp.dimensions()).containsKey(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)
                .doesNotContainKey(IndicatorCodes.COUNTRY_DIMENSION);
            assertThat(dp.value()).isEqualTo(2L);
            assertThat(dp.total()).isEqualTo(3L);
        });
    }

    @Test
    void areasCountGroupedByInitiativeTypeOmitsPausedAreas() {
        var handler = handlerWithPausedQuestions(year -> Set.of("Question74", "Question78"),
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");

        var result = handler.execute(plan(monitoredAreasCountDefinition(), List.of(SURVEY_YEAR_2023),
            List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION)), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(10);
        assertThat(result.exclusions()).hasSize(2);
    }

    @Test
    void policyAreasCountSumsYesAnswersAcrossEveryPolicyMember() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");

        var result = handler.execute(plan(policyAreasCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.NONE));

        // 12 areas each have a "*_POLICY" member (IMMEDIATE_OA_POLICY is a Publications
        // variant, not counted separately) -> GR answers Yes to all 12, DE to none.
        assertThat(capturedRequests).hasSize(12);
        assertThat(valueFor(result, "GR")).isEqualTo(12L);
        assertThat(valueFor(result, "DE")).isEqualTo(0L);
    }

    @Test
    void softwareOpenSourceSetsCountUsesQuestion73() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"14\"]}]}}]}");

        handler.execute(plan(softwareOpenSourceSetsCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(capturedRequests.get(0)).contains("\"name\":\"eosc.sb.Question73\"", "\"year\":2024");
    }

    // --- Degradation: a survey year / question the tool has no data for must not fail the query ---

    @Test
    void countryInitiativeStatusReturnsAnEmptyResultWhenTheSurveyYearHasNoData() {
        var handler = handlerReturning("{\"datasets\":[]}");
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY,
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2019"))
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).isEmpty();
    }

    @Test
    void financialInvestmentSingleYearReturnsOneNullPointWhenTheSurveyYearHasNoData() {
        var handler = handlerReturning("{\"datasets\":[]}");
        var filters = List.of(
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2019")),
            INVESTMENT_AREA_OA_PUBLICATION
        );

        var result = handler.execute(plan(financialInvestmentDefinition(), filters, List.of(), null, AggregationType.SUM));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isNull();
    }

    @Test
    void financialInvestmentTrendYieldsANullPointForAYearWithoutData() {
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[]}",                                                        // 2022: no data
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"FI\",\"10\"]}]}}]}"      // 2023: real data
        ));
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(INVESTMENT_AREA_OA_PUBLICATION), List.of(), timeRange, AggregationType.SUM));

        assertThat(result.data()).hasSize(2);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get("period")).isEqualTo("2022");
            assertThat(dp.value()).isNull();
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions().get("period")).isEqualTo("2023");
            assertThat(dp.value()).isEqualTo(10_000_000.0);
        });
    }

    @Test
    void countryInitiativeStatusWithTimeSeriesReturnsOneStatusPerCountryPerYear() {
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"No\"]},{\"row\":[\"DE\",\"Yes\"]}]}}]}",  // 2022
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"Yes\"]}]}}]}"   // 2023
        ));
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY);
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), timeRange, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(2);
        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION, "period");
        assertThat(result.data()).hasSize(4);
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions()).containsEntry(IndicatorCodes.COUNTRY_DIMENSION, "GR").containsEntry("period", "2022");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_NO);
        });
        assertThat(result.data()).anySatisfy(dp -> {
            assertThat(dp.dimensions()).containsEntry(IndicatorCodes.COUNTRY_DIMENSION, "GR").containsEntry("period", "2023");
            assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_YES);
        });
    }

    @Test
    void countryInitiativeStatusWithTimeSeriesEmitsNoPointForAYearWithNoData() {
        // The handler simply omits the empty year; TimeSeriesNormalizer NULL_FILLs it (see TimeSeriesNormalizerTest).
        var handler = handlerReturningEachInTurn(List.of(
            "{\"datasets\":[]}",                                                          // 2022: no data
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}"       // 2023: real data
        ));
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY);
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), timeRange, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions()).containsEntry("period", "2023");
    }

    @Test
    void countryInitiativeStatusWithTimeSeriesDegradesAnHttp422YearToNoPointsRatherThanFailing() {
        // The live survey stats-tool answers an unavailable eosc.sb.<question> with 422.
        var handler = handlerReturning422ForYear(2023,
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY);
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), timeRange, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions()).containsEntry("period", "2023");
    }

    @Test
    void eachReferenceYearQueriesItsSurveysStatsToolYearAndKeepsTheReferenceYearAsThePeriod() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var timeRange = new ResolvedTimeRange(2021, 2024, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY),
            List.of(), timeRange, AggregationType.NONE));

        assertThat(capturedRequests).hasSize(4);
        assertThat(capturedRequests.get(0)).contains("\"year\":2022");
        assertThat(capturedRequests.get(3)).contains("\"year\":2025");
        assertThat(result.data()).extracting(point -> point.dimensions().get("period"))
            .containsExactly("2021", "2022", "2023", "2024");
    }

    @Test
    void yearsWithoutASurveyMakeNoStatsToolCall() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var timeRange = new ResolvedTimeRange(2025, 2026, MissingPeriodHandling.NULL_FILL);

        handler.execute(plan(countryInitiativeStatusDefinition(), List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY),
            List.of(), timeRange, AggregationType.NONE));

        assertThat(capturedRequests).isEmpty();
    }

    @Test
    void localHandlerReadsTheSurveyListedForTheReferenceYear() {
        SurveyService surveyService = mock(SurveyService.class);
        when(surveyService.getQuestionAnswersByCountry("m-jlFggsCN", "Question6"))
            .thenReturn(List.of(List.of("GR", "Yes")));
        var handler = new LocalSurveyStatsIndicatorQueryHandler(surveyService);
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY,
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2021"))
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_YES);
    }

    @Test
    void localHandlerReturnsNoRowsForAYearWithoutASurvey() {
        SurveyService surveyService = mock(SurveyService.class);
        var handler = new LocalSurveyStatsIndicatorQueryHandler(surveyService);
        var filters = List.of(
            INITIATIVE_TYPE_OA_PUBLICATION_POLICY,
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2020"))
        );

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).isEmpty();
        verifyNoInteractions(surveyService);
    }

    @Test
    void countryPolicyStatusWithTimeSeriesAndInitiativeTypeGroupByFansOutPerCountryPerTypePerYear() {
        // Every fetch (base + mandatory, per type, per year) returns GR=Yes/DE=No; mandatory GR=Yes -> GR MANDATORY, DE NO_POLICY.
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.INITIATIVE_TYPE_DIMENSION));
        var filters = List.of(new ResolvedFilter(
            IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.IN, List.of("OA_PUBLICATION_POLICY", "OPEN_DATA_POLICY")));
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(countryPolicyStatusDefinition(), filters, groupBy, timeRange, AggregationType.NONE));

        assertThat(result.dimensions()).containsExactlyInAnyOrder(
            IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.INITIATIVE_TYPE_DIMENSION, "period");
        // 2 countries x 2 types x 2 years
        assertThat(result.data()).hasSize(8);
        assertThat(result.data()).filteredOn(dp -> dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION).equals("GR"))
            .allSatisfy(dp -> assertThat(dp.value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.POLICY_STATUS_MANDATORY));
    }

    @Test
    void monitoredAreasCountWithTimeSeriesAndCountryGroupByReturnsOneCountPerCountryPerYear() {
        var handler = handlerReturning("{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");
        var groupBy = List.of(new ResolvedDimension(IndicatorCodes.COUNTRY_DIMENSION));
        var timeRange = new ResolvedTimeRange(2022, 2023, MissingPeriodHandling.NULL_FILL);

        var result = handler.execute(plan(monitoredAreasCountDefinition(), List.of(), groupBy, timeRange, AggregationType.NONE));

        assertThat(result.dimensions()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION, "period");
        assertThat(result.data()).hasSize(4);
        assertThat(result.data()).filteredOn(dp -> dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION).equals("GR"))
            .allSatisfy(dp -> assertThat(dp.value()).isEqualTo(12L)); // Yes for all 12 monitoring members
        assertThat(result.data()).filteredOn(dp -> dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION).equals("DE"))
            .allSatisfy(dp -> assertThat(dp.value()).isEqualTo(0L));
    }

    @Test
    void fetchDoublesTreatsANonNumericAnswerAsNoAnswer() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"n/a\"]},{\"row\":[\"FI\",\"10\"]}]}}]}");

        var result = handler.execute(plan(
            financialInvestmentDefinition(), List.of(SURVEY_YEAR_2023, INVESTMENT_AREA_OA_PUBLICATION), List.of(), null, AggregationType.SUM));

        assertThat(result.data().get(0).value()).isEqualTo(10_000_000.0);
    }

    @Test
    void fetchCountsTreatsANonNumericAnswerAsNoAnswer() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"PL\",\"lots\"]},{\"row\":[\"RS\",\"5\"]}]}}]}");

        var result = handler.execute(plan(
            longTermPreservationCountDefinition(), List.of(SURVEY_YEAR_2023), List.of(), null, AggregationType.SUM));

        assertThat(result.data().get(0).value()).isEqualTo(5L);
    }

    @Test
    void initiativeStatusTreatsAnUnexpectedValueAsAwaitingData() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Maybe\"]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).value()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.STATUS_AWAITING_DATA);
    }

    @Test
    void anEmptyRowIsSkippedRatherThanThrowing() {
        var handler = handlerReturning(
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[]},{\"row\":[\"GR\",\"Yes\"]}]}}]}");
        var filters = List.of(INITIATIVE_TYPE_OA_PUBLICATION_POLICY, SURVEY_YEAR_2023);

        var result = handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).dimensions().get(IndicatorCodes.COUNTRY_DIMENSION)).isEqualTo("GR");
    }

    @Test
    void aMissingYearQuestionIsLoggedExactlyOnce() {
        Logger logger = (Logger) LogManager.getLogger(SurveyStatsToolIndicatorQueryHandler.class);
        CapturingAppender appender = new CapturingAppender();
        appender.start();
        logger.addAppender(appender);
        try {
            var handler = handlerReturning("{\"datasets\":[]}");
            var filters = List.of(
                INITIATIVE_TYPE_OA_PUBLICATION_POLICY,
                new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2023"))
            );
            handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));
            handler.execute(plan(countryInitiativeStatusDefinition(), filters, List.of(), null, AggregationType.NONE));

            assertThat(appender.warnings)
                .filteredOn(message -> message.contains("reference year 2023, question Question6"))
                .hasSize(1);
        } finally {
            logger.removeAppender(appender);
        }
    }

    private static final class CapturingAppender extends AbstractAppender {
        private final List<String> warnings = new ArrayList<>();

        private CapturingAppender() {
            super("capture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLevel() == Level.WARN) {
                warnings.add(event.getMessage().getFormattedMessage());
            }
        }
    }
}
