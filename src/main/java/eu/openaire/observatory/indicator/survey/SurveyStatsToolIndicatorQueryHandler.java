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

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;

/**
 * Real handler for the 5 survey-stats-tool-backed indicators (oa.financial_investment,
 * oa.country_initiative_status, oa.eu_country_coverage, repositories.long_term_preservation_count,
 * citizen_science.new_projects_count), all sourced from a separate external survey stats-tool
 * (distinct from the OpenAIRE one — see indicator/openaire/). Scoped to survey years
 * 2022+ only: 2021 used incompatible question numbering (its "Question6"/"Question7" are unrelated
 * numeric funding data, and "Question54"/"Question6.3"/"Question56" aren't registered at all for
 * that year) — confirmed live, not assumed; see docs/INDICATOR_ROADMAP.md. Does not model a real
 * time series: each query resolves to a single survey year (the requested time range's end year,
 * or the latest known year if none is given), not a multi-year OpenAIRE-style range — but every
 * returned data point still stamps a "period" dimension for that resolved year (matching
 * OpenAireStatsToolIndicatorQueryHandler's own PERIOD_DIMENSION convention), because
 * TimeSeriesNormalizer gap-fills any data point it can't match against the requested range's
 * generated periods — without this stamp it would treat every real value as "missing" and zero it
 * out whenever a caller supplies a timeSeries range.
 *
 * <p>oa.country_initiative_status (ATTRIBUTE) and oa.eu_country_coverage (RATIO) are two
 * renderings of the same per-initiativeType Yes/No dataset — one raw per-country map, one turned
 * into a percentage — so both share {@link #fetchInitiativeStatusByCountry} rather than issuing
 * duplicate stats-tool calls. oa.eu_country_coverage's denominator is that year's raw survey
 * respondent count (matching observatory-ui's own calculatePercentage exactly, including
 * countries that gave no answer), not a fixed EU-membership population — see
 * docs/INDICATOR_ROADMAP.md for why this isn't actually filtered to EU countries.
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

    /**
     * Latest survey year confirmed live to carry all 4 initiativeType question codes, Question56,
     * Question89, and Question101 — used when a query doesn't specify a time range. 2021 is
     * deliberately excluded, see the class Javadoc.
     */
    private static final int DEFAULT_YEAR = 2023;

    private static final String FINANCIAL_INVESTMENT_QUESTION_CODE = "Question56";
    private static final String LONG_TERM_PRESERVATION_QUESTION_CODE = "Question89";
    private static final String CITIZEN_SCIENCE_NEW_PROJECTS_QUESTION_CODE = "Question101";

    /** One survey question per fixed initiativeType member — confirmed live, cross-referenced against observatory-ui's report-chart.configuration.ts. */
    private static final Map<String, String> QUESTION_CODE_BY_INITIATIVE_TYPE = Map.of(
        "OA_PUBLICATION_POLICY", "Question6",
        "IMMEDIATE_OA_POLICY", "Question6.3",
        "FINANCIAL_STRATEGY", "Question7",
        "MONITORING_INITIATIVE", "Question54"
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

        List<IndicatorDataPoint> data = byCountryDataPoints(fetch.byCountry(), periodDimension(plan));
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

        long adopted = fetch.byCountry().values().stream().filter(Boolean::booleanValue).count();
        Object value = RatioPercentage.of(adopted, fetch.totalRespondents());

        return buildResult(plan, dimensionKeysFor(List.of(), plan), List.of(new IndicatorDataPoint(periodDimension(plan), value)));
    }

    /**
     * Fetches the raw per-country Yes/No rows once; shared by queryCountryInitiativeStatus and
     * queryEuCountryCoverage so the same initiativeType question is never fetched twice.
     * totalRespondents is the raw row count before no-answer rows are filtered out of byCountry.
     */
    private InitiativeStatusFetch fetchInitiativeStatusByCountry(int year, String questionCode) {
        List<List<String>> rows = client.fetchRows(queryBuilder.surveyQuestionSeriesRequest(year, questionCode));

        Map<String, Boolean> byCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (isNoAnswer(rawValue)) {
                continue;
            }
            byCountry.put(country, switch (rawValue) {
                case "Yes" -> true;
                case "No" -> false;
                default -> throw new SurveyStatsToolQueryException(
                    "Unexpected value for %s/%s: %s".formatted(questionCode, country, rawValue));
            });
        }
        return new InitiativeStatusFetch(byCountry, rows.size());
    }

    private record InitiativeStatusFetch(Map<String, Boolean> byCountry, int totalRespondents) {
    }

    /** Applies the requested AggregationPolicy server-side over per-country values still in millions of euros; the aggregated result is scaled x1,000,000 to euros afterward. */
    private IndicatorResult queryFinancialInvestment(IndicatorExecutionPlan plan) {
        List<List<String>> rows = client.fetchRows(
            queryBuilder.surveyQuestionSeriesRequest(resolveYear(plan), FINANCIAL_INVESTMENT_QUESTION_CODE));

        Map<String, Double> millionsByCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (!isNoAnswer(rawValue)) {
                millionsByCountry.put(country, Double.parseDouble(rawValue));
            }
        }

        Map<String, Object> periodDimension = periodDimension(plan);
        List<String> groupByCodes = plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
        if (groupByCodes.contains(COUNTRY_DIMENSION)) {
            Map<String, Object> eurosByCountry = new LinkedHashMap<>();
            millionsByCountry.forEach((country, millions) -> eurosByCountry.put(country, toEuros(millions)));
            return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), byCountryDataPoints(eurosByCountry, periodDimension));
        }

        Double aggregatedMillions = (Double) aggregateNumbers(plan.aggregation(), selectedValues(plan, millionsByCountry), Double::sum, "oa.financial_investment");
        Object aggregated = aggregatedMillions == null ? null : toEuros(aggregatedMillions);
        return buildResult(plan, dimensionKeysFor(List.of(), plan), List.of(new IndicatorDataPoint(periodDimension, aggregated)));
    }

    /** Survey values are collected in millions of euros; the indicator's declared unit is plain currency (euros). */
    private static double toEuros(double millions) {
        return millions * 1_000_000.0;
    }

    /**
     * Shared shape for repositories.long_term_preservation_count (Question89) and
     * citizen_science.new_projects_count (Question101) — both plain per-country integer counts
     * (unlike Question56/oa.financial_investment, no unit scaling applies). Mirrors
     * queryFinancialInvestment's groupBy/aggregate branching.
     */
    private IndicatorResult queryCountByQuestion(IndicatorExecutionPlan plan, String questionCode) {
        List<List<String>> rows = client.fetchRows(queryBuilder.surveyQuestionSeriesRequest(resolveYear(plan), questionCode));

        Map<String, Long> countByCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (!isNoAnswer(rawValue)) {
                countByCountry.put(country, Math.round(Double.parseDouble(rawValue)));
            }
        }

        Map<String, Object> periodDimension = periodDimension(plan);
        List<String> groupByCodes = plan.groupBy().stream().map(ResolvedDimension::dimensionCode).toList();
        if (groupByCodes.contains(COUNTRY_DIMENSION)) {
            return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), byCountryDataPoints(countByCountry, periodDimension));
        }

        Object aggregated = aggregateNumbers(plan.aggregation(), selectedValues(plan, countByCountry), Long::sum, "count-by-question indicator");
        return buildResult(plan, dimensionKeysFor(List.of(), plan), List.of(new IndicatorDataPoint(periodDimension, aggregated)));
    }

    /** Per-country IndicatorDataPoints, one per map entry, each stamped with the resolved period dimension. */
    private List<IndicatorDataPoint> byCountryDataPoints(Map<String, ?> valueByCountry, Map<String, Object> periodDimension) {
        return valueByCountry.entrySet().stream()
            .map(entry -> {
                Map<String, Object> dimensions = new LinkedHashMap<>();
                dimensions.put(COUNTRY_DIMENSION, entry.getKey());
                dimensions.putAll(periodDimension);
                return new IndicatorDataPoint(dimensions, entry.getValue());
            })
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
            case MIN -> values.stream().min(Comparator.naturalOrder()).orElseThrow();
            case MAX -> values.stream().max(Comparator.naturalOrder()).orElseThrow();
            default -> throw new IllegalStateException(
                "Unsupported aggregation for " + indicatorDescription + ": " + aggregation);
        };
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

    /** No timeRange -> the latest known survey year; otherwise the requested range's end year (single-year, not a real series). */
    private int resolveYear(IndicatorExecutionPlan plan) {
        return plan.timeRange() != null ? plan.timeRange().to() : DEFAULT_YEAR;
    }

    /**
     * Stamps the resolved year as a "period" dimension so TimeSeriesNormalizer can match this data
     * point against its generated period list instead of treating it as missing and zero-filling
     * it — empty when the request didn't include a timeSeries range, since the normalizer no-ops
     * in that case too.
     */
    private Map<String, Object> periodDimension(IndicatorExecutionPlan plan) {
        return plan.timeRange() != null ? Map.of(PERIOD_DIMENSION, String.valueOf(resolveYear(plan))) : Map.of();
    }
}
