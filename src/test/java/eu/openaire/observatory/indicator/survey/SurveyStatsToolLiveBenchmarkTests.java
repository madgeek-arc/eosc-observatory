package eu.openaire.observatory.indicator.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import eu.openaire.observatory.service.SurveyService;
import eu.openaire.observatory.statistics.StatsToolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live head-to-head benchmark of {@link SurveyStatsToolIndicatorQueryHandler} (real HTTP call to
 * a local stats-tool instance) against {@link LocalSurveyStatsIndicatorQueryHandler} (real
 * registry read, via the app's own Spring-wired {@link SurveyService}).
 *
 * <p><b>Requires local infrastructure that is not available in CI</b>, which is why this class is
 * named {@code *Tests.java}: Surefire excludes that pattern and only Failsafe (bound to
 * {@code mvn verify}) picks it up (see the {@code maven-surefire-plugin}/{@code maven-failsafe-plugin}
 * config in {@code pom.xml}).
 *
 * <p>Deliberately does NOT extend {@link eu.openaire.observatory.IntegrationTestConfig} — that
 * config spins up an ephemeral, empty Testcontainers Postgres and mocks the Elasticsearch client,
 * which is the opposite of what this test needs. Instead it explicitly loads the real, gitignored
 * project-root {@code application.properties} via {@link TestPropertySource} (real local Postgres
 * on port 5434, real Elasticsearch) — the same file that configures the actual app when run
 * locally. {@code TestPropertySource} properties take the highest precedence in the environment,
 * so this reliably wins over {@code src/test/resources/application.properties} (which is still on
 * the test classpath and would otherwise leak in HSQLDB/disabled-ES defaults for any key the root
 * file doesn't itself define) rather than depending on Spring Boot's implicit config-location
 * search order, which is what this test relied on before and which silently let
 * {@code registry.elasticsearch.enabled=false} from the test classpath win (the root file never
 * sets that key at all, so "matchIfMissing" never got a chance to apply) — hence the explicit
 * {@code registry.elasticsearch.enabled=true} inline override below.
 *
 * <p>Also requires a local stats-tool (~/Projects/Statistic-chart-generator) running at
 * {@code http://localhost:8080/stats-api/} with the {@code eosc-obs} profile configured against
 * the same registry DB, and serving the year-namespaced named queries this handler still sends
 * (e.g. "eosc.sb.2024.Question6") — adjust {@link #SURVEY_YEAR} below to a year with real
 * validated answers in your local DB if 2024 doesn't have any.
 *
 * <p>Only exercises {@code OA_PUBLICATION_POLICY} (Question6, a flat {@code answer.*.Question6.
 * Question6-0} shape). {@code IMMEDIATE_OA_POLICY} (Question6.3) is NOT exercised here: its real
 * named query reads a 3-level-deep nested path ({@code answer.*.Question6.Question6-1-0.
 * Question6-3}) that {@code SurveyServiceImpl.extractAnswerValue}'s flat 2-level lookup does not
 * handle, so the two handlers would disagree for that initiativeType.
 */
@SpringBootTest
@TestPropertySource(
    locations = "file:application.properties",
    properties = "registry.elasticsearch.enabled=true"
)
class SurveyStatsToolLiveBenchmarkTests {

    private static final String LOCAL_STATS_TOOL_ENDPOINT = "http://localhost:8080/stats-api/";
    private static final String SURVEY_PROFILE = "eosc-obs";
    private static final int SURVEY_YEAR = 2024;
    private static final String INITIATIVE_TYPE = "OA_PUBLICATION_POLICY";

    private static final int WARMUP_ITERATIONS = 5;
    private static final int MEASURED_ITERATIONS = 20;

    @Autowired
    private SurveyService surveyService;

    @Test
    void localAndRemoteImplementationsAgreeOnRealDataAndAreBenchmarked() {
        SurveyStatsToolIndicatorQueryHandler remoteHandler = remoteHandler();
        LocalSurveyStatsIndicatorQueryHandler localHandler = new LocalSurveyStatsIndicatorQueryHandler(surveyService);
        IndicatorExecutionPlan plan = plan();

        IndicatorResult remoteResult = remoteHandler.execute(plan);
        IndicatorResult localResult = localHandler.execute(plan);
        assertThat(toComparableRows(localResult)).containsExactlyInAnyOrderElementsOf(toComparableRows(remoteResult));

        double remoteMeanMillis = benchmark(() -> remoteHandler.execute(plan));
        double localMeanMillis = benchmark(() -> localHandler.execute(plan));

        System.out.printf(
            "%n[SurveyStatsToolLiveBenchmarkTests] survey %d, initiativeType %s, %d measured iterations after %d warm-up:%n"
                + "  remote (%s) : %.2f ms/call%n"
                + "  local  (registry via SurveyService): %.2f ms/call%n%n",
            SURVEY_YEAR, INITIATIVE_TYPE, MEASURED_ITERATIONS, WARMUP_ITERATIONS,
            LOCAL_STATS_TOOL_ENDPOINT, remoteMeanMillis, localMeanMillis);
    }

    private static List<List<Object>> toComparableRows(IndicatorResult result) {
        return result.data().stream()
            .map(dp -> List.of(dp.dimensions().get(IndicatorCodes.COUNTRY_DIMENSION), dp.value()))
            .toList();
    }

    private static double benchmark(Runnable call) {
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            call.run();
        }
        long start = System.nanoTime();
        for (int i = 0; i < MEASURED_ITERATIONS; i++) {
            call.run();
        }
        long elapsedNanos = System.nanoTime() - start;
        return (elapsedNanos / 1_000_000.0) / MEASURED_ITERATIONS;
    }

    private static SurveyStatsToolIndicatorQueryHandler remoteHandler() {
        StatsToolProperties properties = new StatsToolProperties()
            .setEndpoint(LOCAL_STATS_TOOL_ENDPOINT)
            .setSurveyProfile(SURVEY_PROFILE);
        SurveyStatsToolClient client = new SurveyStatsToolClient(WebClient.builder(), properties);
        SurveyStatsToolQueryBuilder queryBuilder = new SurveyStatsToolQueryBuilder(new ObjectMapper(), properties);
        return new SurveyStatsToolIndicatorQueryHandler(queryBuilder, client, PausedSurveyQuestions.none());
    }

    private static IndicatorExecutionPlan plan() {
        List<ResolvedFilter> filters = List.of(
            new ResolvedFilter(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of(INITIATIVE_TYPE)),
            new ResolvedFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of(String.valueOf(SURVEY_YEAR)))
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
