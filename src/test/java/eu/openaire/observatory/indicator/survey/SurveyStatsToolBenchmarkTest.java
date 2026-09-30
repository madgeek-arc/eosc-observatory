package eu.openaire.observatory.indicator.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.domain.Stakeholder;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorFormat;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorExecutionBinding;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.model.IndicatorStatus;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.NullHandling;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.QuerySecurityScope;
import eu.openaire.observatory.indicator.validation.ResolvedFilter;
import eu.openaire.observatory.service.CrudService;
import eu.openaire.observatory.service.SurveyServiceImpl;
import eu.openaire.observatory.statistics.StatsToolProperties;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.catalogue.service.ModelService;
import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Benchmarks {@link SurveyStatsToolIndicatorQueryHandler} (remote, stubbed HTTP) against
 * {@link LocalSurveyStatsIndicatorQueryHandler} (local, mocked registry) on an identical
 * synthetic dataset. No live network or database is available in this environment, so both
 * backends are stubbed — this measures each handler's own in-JVM processing overhead (HTTP
 * envelope/JSON (de)serialization vs. in-memory JSON-object walking + per-row stakeholder lookup),
 * not real network/DB latency. It is not a substitute for a true end-to-end comparison, which
 * would need to run against the live {@code stats-tool.endpoint} and a real {@code registry} DB.
 */
class SurveyStatsToolBenchmarkTest {

    private static final int RESPONDENT_COUNT = 50;
    private static final int WARMUP_ITERATIONS = 20;
    private static final int MEASURED_ITERATIONS = 200;

    private static final List<String> COUNTRIES =
        IntStream.range(0, RESPONDENT_COUNT).mapToObj(i -> "C" + i).toList();

    @Test
    void localAndRemoteImplementationsAgreeAndLocalIsBenchmarkedAgainstRemote() {
        SurveyStatsToolIndicatorQueryHandler remoteHandler = remoteHandler();
        LocalSurveyStatsIndicatorQueryHandler localHandler = localHandler();
        IndicatorExecutionPlan plan = plan();

        // Correctness: both implementations must agree on the same synthetic dataset before their
        // timings are worth comparing at all.
        IndicatorResult remoteResult = remoteHandler.execute(plan);
        IndicatorResult localResult = localHandler.execute(plan);
        assertThat(toComparableRows(localResult)).containsExactlyInAnyOrderElementsOf(toComparableRows(remoteResult));

        double remoteMeanMicros = benchmark(() -> remoteHandler.execute(plan));
        double localMeanMicros = benchmark(() -> localHandler.execute(plan));

        System.out.printf(
            "%n[SurveyStatsToolBenchmarkTest] %d respondents, %d measured iterations after %d warm-up:%n"
                + "  remote (stubbed HTTP) : %.2f us/call%n"
                + "  local  (mocked registry): %.2f us/call%n%n",
            RESPONDENT_COUNT, MEASURED_ITERATIONS, WARMUP_ITERATIONS, remoteMeanMicros, localMeanMicros);
    }

    private static List<List<Object>> toComparableRows(IndicatorResult result) {
        return result.data().stream()
            .map(dp -> List.of(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION), dp.value()))
            .toList();
    }

    /** Runs {@code call} WARMUP_ITERATIONS times unmeasured, then MEASURED_ITERATIONS times measured; returns mean microseconds/call. */
    private static double benchmark(Runnable call) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            call.run();
        }
        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            call.run();
        }
        long elapsedNanos = System.nanoTime() - start;
        return (elapsedNanos / 1000.0) / MEASURED_ITERATIONS;
    }

    private static String answerFor(int index) {
        return index % 2 == 0 ? "Yes" : "No";
    }

    // --- remote (stubbed HTTP) handler setup, same pattern as SurveyStatsToolIndicatorQueryHandlerTest ---

    private SurveyStatsToolIndicatorQueryHandler remoteHandler() {
        String rows = IntStream.range(0, RESPONDENT_COUNT)
            .mapToObj(i -> "{\"row\":[\"%s\",\"%s\"]}".formatted(COUNTRIES.get(i), answerFor(i)))
            .collect(Collectors.joining(","));
        String responseJson = "{\"datasets\":[{\"series\":{\"result\":[" + rows + "]}}]}";

        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
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
        return new SurveyStatsToolIndicatorQueryHandler(queryBuilder, client, PausedSurveyQuestions.none());
    }

    // --- local (mocked registry) handler setup ---

    @SuppressWarnings("unchecked")
    private LocalSurveyStatsIndicatorQueryHandler localHandler() {
        CrudService<Stakeholder> stakeholderCrudService = mock(CrudService.class);
        CrudService<SurveyAnswer> surveyAnswerCrudService = mock(CrudService.class);

        List<SurveyAnswer> answers = IntStream.range(0, RESPONDENT_COUNT)
            .mapToObj(i -> {
                SurveyAnswer answer = new SurveyAnswer();
                answer.setId("sa-" + i);
                answer.setStakeholderId("sh-" + i);
                answer.setValidated(true);
                JSONObject question = new JSONObject();
                question.put("Question6-0", answerFor(i));
                JSONObject section = new JSONObject();
                section.put("Question6", question);
                JSONObject root = new JSONObject();
                root.put("section1", section);
                answer.setAnswer(root);
                return answer;
            })
            .toList();
        when(surveyAnswerCrudService.getAll(any(FacetFilter.class)))
            .thenReturn(new Browsing<>(answers.size(), 0, answers.size(), answers, List.of()));

        List<Stakeholder> stakeholders = IntStream.range(0, RESPONDENT_COUNT)
            .mapToObj(i -> {
                Stakeholder stakeholder = new Stakeholder();
                stakeholder.setId("sh-" + i);
                stakeholder.setCountry(COUNTRIES.get(i));
                return stakeholder;
            })
            .toList();
        when(stakeholderCrudService.getAll(any(FacetFilter.class)))
            .thenReturn(new Browsing<>(stakeholders.size(), 0, stakeholders.size(), stakeholders, List.of()));

        SurveyServiceImpl surveyService = new SurveyServiceImpl(
            stakeholderCrudService,
            surveyAnswerCrudService,
            mock(GenericResourceService.class),
            mock(eu.openaire.observatory.permissions.PermissionService.class),
            mock(ModelService.class),
            mock(eu.openaire.observatory.service.UserService.class),
            new ObjectMapper(),
            mock(eu.openaire.observatory.service.CacheService.class),
            mock(eu.openaire.observatory.service.EmailSurveyService.class),
            mock(eu.openaire.observatory.service.SurveySettingsService.class),
            mock(eu.openaire.observatory.service.SurveyAnswerLocks.class)
        );
        return new LocalSurveyStatsIndicatorQueryHandler(surveyService);
    }

    // --- shared execution plan / indicator definition ---

    private static IndicatorExecutionPlan plan() {
        List<ResolvedFilter> filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_POLICY")),
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of("2024"))
        );
        return new IndicatorExecutionPlan(
            countryInitiativeStatusDefinition(), AggregationType.NONE, filters, List.of(), null,
            null, QuerySecurityScope.unrestricted(), 1000, List.of()
        );
    }

    private static IndicatorDefinition countryInitiativeStatusDefinition() {
        return new IndicatorDefinition(
            UUID.randomUUID(), IndicatorCodes.COUNTRY_INITIATIVE_STATUS, "Country Initiative Status", "desc",
            IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, IndicatorFormat.YES_NO, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    IndicatorCodes.INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
                ),
                new IndicatorDimensionBinding(
                    IndicatorCodes.SURVEY_YEAR_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
                )
            ),
            new TimePolicy(false, MissingPeriodHandling.OMIT),
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, IndicatorCodes.COUNTRY_INITIATIVE_STATUS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
