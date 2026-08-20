package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.handler.AbstractIndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.RatioPercentage;
import eu.openaire.observatory.indicator.handler.RemoteIndicatorClient;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.IntStream;

/**
 * Real handler for the survey-stats-tool-backed indicators (oa.financial_investment,
 * oa.country_initiative_status, oa.eu_country_coverage, repositories.long_term_preservation_count,
 * citizen_science.new_projects_count), all sourced from a separate external survey stats-tool
 * (distinct from the OpenAIRE one — see indicator/openaire/). oa.financial_investment and
 * oa.country_initiative_status/oa.eu_country_coverage are cross-topic despite the "oa." prefix —
 * required investmentArea/initiativeType filters pick which topic's question is queried (see
 * QUESTION_CODE_BY_INVESTMENT_AREA/QUESTION_CODE_BY_INITIATIVE_TYPE below). Scoped to survey years
 * 2022+ only: 2021 used incompatible question numbering (its "Question6"/"Question7" are unrelated
 * numeric funding data, and "Question54"/"Question6.3"/"Question56" aren't registered at all for
 * that year) — confirmed live, not assumed.
 *
 * <p>oa.financial_investment, repositories.long_term_preservation_count, and
 * citizen_science.new_projects_count support a real multi-year trend (TimePolicy(supported=true)):
 * when the plan carries a timeSeries range, {@link #resolveYears} fetches one point per year instead
 * of requiring a single surveyYear filter, mirroring OpenAireStatsToolIndicatorQueryHandler's own
 * period-per-year loop. oa.country_initiative_status and oa.eu_country_coverage remain single-survey
 * snapshots (TimePolicy(supported=false)) — every query still resolves exactly one survey year via
 * the required surveyYear filter (there's no "latest survey" concept to default to, same as surveyId
 * elsewhere in this catalog).
 *
 * <p>oa.country_initiative_status (ATTRIBUTE) and oa.eu_country_coverage (RATIO) are two
 * renderings of the same per-initiativeType Yes/No dataset — one raw per-country map, one turned
 * into a percentage — so both share {@link #fetchInitiativeStatusByCountry} rather than issuing
 * duplicate stats-tool calls. oa.eu_country_coverage's denominator is that year's raw survey
 * respondent count (matching observatory-ui's own calculatePercentage exactly, including
 * countries that gave no answer), not a fixed EU-membership population — despite the name, this
 * matches the legacy system's own (non-EU-scoped) definition rather than correcting it, so it
 * isn't actually filtered to EU countries.
 *
 * <p>repositories.long_term_preservation_count (Question89) and citizen_science.new_projects_count
 * (Question101) are plain per-country integer counts, structurally identical to
 * oa.financial_investment apart from having no unit scale — both share
 * {@link #queryCountByQuestion}.
 */
@Service
public class SurveyStatsToolIndicatorQueryHandler extends AbstractIndicatorQueryHandler implements RemoteIndicatorClient {

    public static final String HANDLER_KEY = "survey-stats-tool";

    private static final String COUNTRY_DIMENSION = IndicatorCodes.COUNTRY_DIMENSION;
    private static final String INITIATIVE_TYPE_DIMENSION = IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
    private static final String INVESTMENT_AREA_DIMENSION = IndicatorCodes.INVESTMENT_AREA_DIMENSION;
    private static final String SURVEY_YEAR_DIMENSION = IndicatorCodes.SURVEY_YEAR_DIMENSION;

    private static final String LONG_TERM_PRESERVATION_QUESTION_CODE = "Question89";
    private static final String CITIZEN_SCIENCE_NEW_PROJECTS_QUESTION_CODE = "Question101";

    /** The 3 values a country_initiative_status data point can carry — see IndicatorCatalog.countryInitiativeStatus(). */
    public static final String STATUS_YES = "YES";
    public static final String STATUS_NO = "NO";
    public static final String STATUS_AWAITING_DATA = "AWAITING_DATA";

    /**
     * One survey question per fixed initiativeType member — confirmed live, cross-referenced
     * against observatory-ui's report-chart.configuration.ts (OA_PUBLICATION_POLICY/
     * IMMEDIATE_OA_POLICY/FINANCIAL_STRATEGY/MONITORING_INITIATIVE) and, for the Open Data/FAIR Data
     * members, against the real m-eosc-sb-2022/2023/2024/2025 registry survey models directly (same
     * question numbering confirmed stable across all 4 years) plus a live raw?json= call per code.
     */
    private static final Map<String, String> QUESTION_CODE_BY_INITIATIVE_TYPE = Map.ofEntries(
        Map.entry("OA_PUBLICATION_POLICY", "Question6"),
        Map.entry("IMMEDIATE_OA_POLICY", "Question6.3"),
        Map.entry("FINANCIAL_STRATEGY", "Question7"),
        Map.entry("MONITORING_INITIATIVE", "Question54"),
        Map.entry("OPEN_DATA_POLICY", "Question18"),
        Map.entry("OPEN_DATA_FINANCIAL_STRATEGY", "Question19"),
        Map.entry("OPEN_DATA_MONITORING_INITIATIVE", "Question66"),
        Map.entry("FAIR_DATA_POLICY", "Question14"),
        Map.entry("FAIR_DATA_FINANCIAL_STRATEGY", "Question15"),
        Map.entry("FAIR_DATA_MONITORING_INITIATIVE", "Question62")
    );

    /**
     * One survey question per investmentArea member — a separate numeric ("in millions of Euros")
     * question series from QUESTION_CODE_BY_INITIATIVE_TYPE's Yes/No ones. Confirmed live against
     * the real m-eosc-sb-2022/2023/2024/2025 registry survey models (same numbering across all 4
     * years) plus a live raw?json= call per code.
     */
    private static final Map<String, String> QUESTION_CODE_BY_INVESTMENT_AREA = Map.of(
        "OA_PUBLICATION", "Question56",
        "OPEN_DATA", "Question68",
        "FAIR_DATA", "Question64"
    );

    private final SurveyStatsToolQueryBuilder queryBuilder;
    private final SurveyStatsToolClient client;

    public SurveyStatsToolIndicatorQueryHandler(SurveyStatsToolQueryBuilder queryBuilder, SurveyStatsToolClient client) {
        this.queryBuilder = queryBuilder;
        this.client = client;
    }

    @Override
    public String handlerKey() {
        return HANDLER_KEY;
    }

    @Override
    public IndicatorResult execute(IndicatorExecutionPlan plan) {
        return query(plan);
    }

    @Override
    public IndicatorResult query(IndicatorExecutionPlan plan) {
        String providerIndicatorKey = plan.definition().executionBinding().providerIndicatorKey();
        return switch (providerIndicatorKey) {
            case IndicatorCodes.FINANCIAL_INVESTMENT -> queryFinancialInvestment(plan);
            case IndicatorCodes.COUNTRY_INITIATIVE_STATUS -> queryCountryInitiativeStatus(plan);
            case IndicatorCodes.EU_COUNTRY_COVERAGE -> queryEuCountryCoverage(plan);
            case IndicatorCodes.LONG_TERM_PRESERVATION_COUNT -> queryCountByQuestion(plan, LONG_TERM_PRESERVATION_QUESTION_CODE);
            case IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT -> queryCountByQuestion(plan, CITIZEN_SCIENCE_NEW_PROJECTS_QUESTION_CODE);
            default -> throw new IllegalStateException(
                "SurveyStatsToolIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
        };
    }

    /** One row per country that answered; country grouping is inherent to this indicator's meaning, not driven by plan.groupBy(). */
    private IndicatorResult queryCountryInitiativeStatus(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInitiativeType(requiredFilterValue(plan, INITIATIVE_TYPE_DIMENSION));
        InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(resolveYear(plan), questionCode);

        List<IndicatorDataPoint> data = byCountryDataPoints(fetch.byCountry());
        return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), data);
    }

    /**
     * Denominator is that year's raw survey respondent count for this question (matching
     * observatory-ui's own calculatePercentage exactly) — a country with no answer still counts
     * toward the denominator, it's just not counted as adopting.
     */
    private IndicatorResult queryEuCountryCoverage(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInitiativeType(requiredFilterValue(plan, INITIATIVE_TYPE_DIMENSION));
        InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(resolveYear(plan), questionCode);

        long adopted = fetch.byCountry().values().stream().filter(STATUS_YES::equals).count();
        Object value = RatioPercentage.of(adopted, fetch.totalRespondents());

        return buildResult(plan, dimensionKeysFor(List.of(), plan), List.of(new IndicatorDataPoint(Map.of(), value)));
    }

    /**
     * Fetches the raw per-country Yes/No/no-answer rows once; shared by queryCountryInitiativeStatus
     * and queryEuCountryCoverage so the same initiativeType question is never fetched twice.
     * totalRespondents is the raw row count, which already includes AWAITING_DATA countries.
     * Non-respondent countries are reported as AWAITING_DATA rather than dropped, so a consumer can
     * tell "answered No" apart from "hasn't answered yet" — the survey itself has no richer status
     * than this 3-value set (confirmed live: every policy/monitoring question is a flat Yes/No radio
     * field, see IndicatorCatalog.countryInitiativeStatus() javadoc).
     */
    private InitiativeStatusFetch fetchInitiativeStatusByCountry(int year, String questionCode) {
        List<List<String>> rows = client.fetchRows(queryBuilder.surveyQuestionSeriesRequest(year, questionCode));

        Map<String, String> byCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            byCountry.put(country, isNoAnswer(rawValue) ? STATUS_AWAITING_DATA : switch (rawValue) {
                case "Yes" -> STATUS_YES;
                case "No" -> STATUS_NO;
                default -> throw new SurveyStatsToolQueryException(
                    "Unexpected value for %s/%s: %s".formatted(questionCode, country, rawValue));
            });
        }
        return new InitiativeStatusFetch(byCountry, rows.size());
    }

    private record InitiativeStatusFetch(Map<String, String> byCountry, int totalRespondents) {
    }

    /**
     * Applies the requested AggregationPolicy server-side over per-country values still in millions
     * of euros; each year's aggregated (or per-country) result is scaled x1,000,000 to euros
     * afterward. One data point per year when a timeSeries range was requested (mirrors
     * OpenAireStatsToolIndicatorQueryHandler's own period-per-year loop), otherwise a single point
     * for the year picked by an explicit surveyYear filter.
     */
    private IndicatorResult queryFinancialInvestment(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInvestmentArea(requiredFilterValue(plan, INVESTMENT_AREA_DIMENSION));
        boolean byCountry = groupByCodes(plan).contains(COUNTRY_DIMENSION);

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int year : resolveYears(plan)) {
            Map<String, Double> millionsByCountry = fetchDoublesByCountry(year, questionCode);
            Map<String, Object> periodDimensions = periodDimensions(plan, year);

            if (byCountry) {
                millionsByCountry.forEach((country, millions) -> {
                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    dimensions.put(COUNTRY_DIMENSION, country);
                    data.add(new IndicatorDataPoint(dimensions, toEuros(millions)));
                });
            } else {
                Double aggregatedMillions = (Double) aggregateNumbers(
                    plan.aggregation(), selectedValues(plan, millionsByCountry), Double::sum, "oa.financial_investment");
                Object aggregated = aggregatedMillions == null ? null : toEuros(aggregatedMillions);
                data.add(new IndicatorDataPoint(periodDimensions, aggregated));
            }
        }
        return buildResult(plan, dimensionKeysFor(byCountry ? List.of(COUNTRY_DIMENSION) : List.of(), plan), data);
    }

    private Map<String, Double> fetchDoublesByCountry(int year, String questionCode) {
        List<List<String>> rows = client.fetchRows(queryBuilder.surveyQuestionSeriesRequest(year, questionCode));
        Map<String, Double> byCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (!isNoAnswer(rawValue)) {
                byCountry.put(country, Double.parseDouble(rawValue));
            }
        }
        return byCountry;
    }

    /** Survey values are collected in millions of euros; the indicator's declared unit is plain currency (euros). */
    private static double toEuros(double millions) {
        return millions * 1_000_000.0;
    }

    /**
     * Shared shape for repositories.long_term_preservation_count (Question89) and
     * citizen_science.new_projects_count (Question101) — both plain per-country integer counts
     * (unlike Question56/oa.financial_investment, no unit scaling applies). Mirrors
     * queryFinancialInvestment's groupBy/aggregate/multi-year branching.
     */
    private IndicatorResult queryCountByQuestion(IndicatorExecutionPlan plan, String questionCode) {
        boolean byCountry = groupByCodes(plan).contains(COUNTRY_DIMENSION);

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int year : resolveYears(plan)) {
            Map<String, Long> countByCountry = fetchCountsByCountry(year, questionCode);
            Map<String, Object> periodDimensions = periodDimensions(plan, year);

            if (byCountry) {
                countByCountry.forEach((country, value) -> {
                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    dimensions.put(COUNTRY_DIMENSION, country);
                    data.add(new IndicatorDataPoint(dimensions, value));
                });
            } else {
                Object aggregated = aggregateNumbers(
                    plan.aggregation(), selectedValues(plan, countByCountry), Long::sum, "count-by-question indicator");
                data.add(new IndicatorDataPoint(periodDimensions, aggregated));
            }
        }
        return buildResult(plan, dimensionKeysFor(byCountry ? List.of(COUNTRY_DIMENSION) : List.of(), plan), data);
    }

    private Map<String, Long> fetchCountsByCountry(int year, String questionCode) {
        List<List<String>> rows = client.fetchRows(queryBuilder.surveyQuestionSeriesRequest(year, questionCode));
        Map<String, Long> byCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (!isNoAnswer(rawValue)) {
                byCountry.put(country, Math.round(Double.parseDouble(rawValue)));
            }
        }
        return byCountry;
    }

    private static List<String> groupByCodes(IndicatorExecutionPlan plan) {
        return plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
    }

    /** No timeRange -> no period dimension at all (matches the non-trend indicators' existing shape); otherwise one "period" dimension per year, mirroring OpenAireStatsToolIndicatorQueryHandler's own period handling. */
    private static Map<String, Object> periodDimensions(IndicatorExecutionPlan plan, int year) {
        return plan.timeRange() == null ? Map.of() : Map.of(PERIOD_DIMENSION, String.valueOf(year));
    }

    /** Per-country IndicatorDataPoints, one per map entry. */
    private List<IndicatorDataPoint> byCountryDataPoints(Map<String, ?> valueByCountry) {
        return valueByCountry.entrySet().stream()
            .map(entry -> new IndicatorDataPoint(Map.of(COUNTRY_DIMENSION, entry.getKey()), entry.getValue()))
            .toList();
    }

    /** The requested countries' values (filtered to those with an answer), or every answering country's value if none were requested. */
    private <V> List<V> selectedValues(IndicatorExecutionPlan plan, Map<String, V> valueByCountry) {
        List<String> requestedCountries = filterValues(plan, COUNTRY_DIMENSION);
        return requestedCountries.isEmpty()
            ? List.copyOf(valueByCountry.values())
            : requestedCountries.stream().map(valueByCountry::get).filter(Objects::nonNull).toList();
    }

    /**
     * Shared SUM/AVG/MIN/MAX switch for both financial-investment (millions of euros, as Double)
     * and plain integer counts (as Long) — SUM/MIN/MAX preserve the input type T, AVG always
     * yields a Double, matching each domain's natural arithmetic.
     */
    private static <T extends Number & Comparable<T>> Object aggregateNumbers(
            AggregationType aggregation, List<T> values, BinaryOperator<T> sum, String indicatorDescription) {
        if (values.isEmpty()) {
            return null;
        }
        return switch (aggregation) {
            case SUM -> values.stream().reduce(sum).orElseThrow();
            case AVG -> values.stream().mapToDouble(Number::doubleValue).average().orElseThrow();
            case MEDIAN -> median(values);
            case MIN -> values.stream().min(Comparator.naturalOrder()).orElseThrow();
            case MAX -> values.stream().max(Comparator.naturalOrder()).orElseThrow();
            default -> throw new IllegalStateException(
                "Unsupported aggregation for " + indicatorDescription + ": " + aggregation);
        };
    }

    /** Even-count lists average the two middle values (always a Double, like AVG) — odd-count lists return the natural middle element untouched (preserving type T, like MIN/MAX). */
    private static <T extends Number & Comparable<T>> Object median(List<T> values) {
        List<T> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1).doubleValue() + sorted.get(middle).doubleValue()) / 2.0;
    }

    /** A real JSON null, the literal string "null", and an empty string were all observed live and all mean "no answer". */
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

    private String questionCodeForInvestmentArea(String investmentArea) {
        String questionCode = QUESTION_CODE_BY_INVESTMENT_AREA.get(investmentArea);
        if (questionCode == null) {
            throw new IllegalStateException("No survey question mapped for investmentArea: " + investmentArea);
        }
        return questionCode;
    }

    /** Required surveyYear filter — there's no "latest survey" concept to default to (same as surveyId elsewhere in this catalog). */
    private int resolveYear(IndicatorExecutionPlan plan) {
        return Integer.parseInt(requiredFilterValue(plan, SURVEY_YEAR_DIMENSION));
    }

    /**
     * Every year in the requested timeSeries range (for the trend-capable indicators —
     * oa.financial_investment, repositories.long_term_preservation_count,
     * citizen_science.new_projects_count), or the single year picked by an explicit surveyYear
     * filter otherwise — there's no "latest survey" concept to default to when neither is given.
     */
    private List<Integer> resolveYears(IndicatorExecutionPlan plan) {
        if (plan.timeRange() != null) {
            return IntStream.rangeClosed(plan.timeRange().from(), plan.timeRange().to()).boxed().toList();
        }
        List<String> yearFilterValues = filterValues(plan, SURVEY_YEAR_DIMENSION);
        if (yearFilterValues.isEmpty()) {
            throw new IllegalStateException(
                "Expected either a timeSeries range or a surveyYear filter to already have been validated");
        }
        return List.of(Integer.parseInt(yearFilterValues.get(0)));
    }
}
