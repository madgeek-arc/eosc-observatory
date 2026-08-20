package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.handler.AbstractIndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.RatioPercentage;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.service.SurveyService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local (in-process, no external HTTP call) equivalent of {@link SurveyStatsToolIndicatorQueryHandler}'s
 * {@code oa.country_initiative_status}/{@code oa.eu_country_coverage} logic, reading the same
 * {@code survey_answer} data straight from this app's own registry via {@link SurveyService}
 * instead of a remote survey-stats-tool named query. Built to benchmark against the existing
 * remote handler — deliberately NOT wired into {@code IndicatorCatalog}, so it serves no live
 * traffic and {@link SurveyStatsToolIndicatorQueryHandler} is untouched and remains the real
 * handler for these indicators.
 *
 * <p>Question-code-to-initiativeType mapping and Yes/No row processing are duplicated from
 * {@link SurveyStatsToolIndicatorQueryHandler} rather than shared, so each handler does its own
 * real work end-to-end and a benchmark between the two is apples-to-apples.
 */
@Service
public class LocalSurveyStatsIndicatorQueryHandler extends AbstractIndicatorQueryHandler implements IndicatorQueryHandler {

    public static final String HANDLER_KEY = "local-survey-stats";

    private static final String COUNTRY_DIMENSION = IndicatorCodes.COUNTRY_DIMENSION;
    private static final String INITIATIVE_TYPE_DIMENSION = IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
    private static final String SURVEY_YEAR_DIMENSION = IndicatorCodes.SURVEY_YEAR_DIMENSION;

    private static final String SURVEY_ID_PREFIX = "m-eosc-sb-";

    /** One survey question per fixed initiativeType member — same mapping as SurveyStatsToolIndicatorQueryHandler. */
    private static final Map<String, String> QUESTION_CODE_BY_INITIATIVE_TYPE = Map.of(
        "OA_PUBLICATION_POLICY", "Question6",
        "IMMEDIATE_OA_POLICY", "Question6.3",
        "FINANCIAL_STRATEGY", "Question7",
        "MONITORING_INITIATIVE", "Question54"
    );

    private final SurveyService surveyService;

    public LocalSurveyStatsIndicatorQueryHandler(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        String providerIndicatorKey = plan.definition().executionBinding().providerIndicatorKey();
        return switch (providerIndicatorKey) {
            case IndicatorCodes.COUNTRY_INITIATIVE_STATUS -> queryCountryInitiativeStatus(plan);
            case IndicatorCodes.EU_COUNTRY_COVERAGE -> queryEuCountryCoverage(plan);
            default -> throw new IllegalStateException(
                "LocalSurveyStatsIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
        };
    }

    private IndicatorResult queryCountryInitiativeStatus(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInitiativeType(requiredFilterValue(plan, INITIATIVE_TYPE_DIMENSION));
        InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(resolveYear(plan), questionCode);

        List<IndicatorDataPoint> data = byCountryDataPoints(fetch.byCountry());
        return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), data);
    }

    private IndicatorResult queryEuCountryCoverage(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInitiativeType(requiredFilterValue(plan, INITIATIVE_TYPE_DIMENSION));
        InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(resolveYear(plan), questionCode);

        long adopted = fetch.byCountry().values().stream()
            .filter(SurveyStatsToolIndicatorQueryHandler.STATUS_YES::equals).count();
        Object value = RatioPercentage.of(adopted, fetch.totalRespondents());

        return buildResult(plan, dimensionKeysFor(List.of(), plan), List.of(new IndicatorDataPoint(Map.of(), value)));
    }

    /**
     * Fetches the raw per-country Yes/No/no-answer rows locally, once; shared by both query
     * methods. Non-respondent countries are reported as AWAITING_DATA rather than dropped, matching
     * SurveyStatsToolIndicatorQueryHandler's own 3-state behavior.
     */
    private InitiativeStatusFetch fetchInitiativeStatusByCountry(int year, String questionCode) {
        String surveyId = SURVEY_ID_PREFIX + year;
        List<List<String>> rows = surveyService.getQuestionAnswersByCountry(surveyId, questionCode);

        Map<String, String> byCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            byCountry.put(country, isNoAnswer(rawValue) ? SurveyStatsToolIndicatorQueryHandler.STATUS_AWAITING_DATA : switch (rawValue) {
                case "Yes" -> SurveyStatsToolIndicatorQueryHandler.STATUS_YES;
                case "No" -> SurveyStatsToolIndicatorQueryHandler.STATUS_NO;
                default -> throw new SurveyStatsToolQueryException(
                    "Unexpected value for %s/%s: %s".formatted(questionCode, country, rawValue));
            });
        }
        return new InitiativeStatusFetch(byCountry, rows.size());
    }

    private record InitiativeStatusFetch(Map<String, String> byCountry, int totalRespondents) {
    }

    private List<IndicatorDataPoint> byCountryDataPoints(Map<String, ?> valueByCountry) {
        return valueByCountry.entrySet().stream()
            .map(entry -> new IndicatorDataPoint(Map.of(COUNTRY_DIMENSION, entry.getKey()), entry.getValue()))
            .toList();
    }

    /** A real JSON null, the literal string "null", and an empty string all mean "no answer" — same as SurveyStatsToolIndicatorQueryHandler. */
    private static boolean isNoAnswer(String rawValue) {
        return rawValue == null || rawValue.isBlank() || rawValue.equals("null");
    }

    private String questionCodeForInitiativeType(String initiativeType) {
        String questionCode = QUESTION_CODE_BY_INITIATIVE_TYPE.get(initiativeType);
        if (questionCode == null) {
            throw new IllegalStateException("No survey question mapped for initiativeType: " + initiativeType);
        }
        return questionCode;
    }

    private int resolveYear(IndicatorExecutionPlan plan) {
        return Integer.parseInt(requiredFilterValue(plan, SURVEY_YEAR_DIMENSION));
    }
}
