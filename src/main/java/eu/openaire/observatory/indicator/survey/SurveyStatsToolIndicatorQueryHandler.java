package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.RemoteIndicatorClient;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Real handler for the 3 survey-stats-tool-backed indicators (oa.financial_investment,
 * oa.country_initiative_status, oa.eu_country_coverage), all sourced from a separate external
 * survey stats-tool (distinct from the OpenAIRE bibliometric one — see indicator/openaire/).
 * Scoped to survey years 2022+ only: 2021 used incompatible question numbering (its "Question6"/
 * "Question7" are unrelated numeric funding data, and "Question54"/"Question6.3"/"Question56"
 * aren't registered at all for that year) — confirmed live, not assumed; see
 * docs/INDICATOR_ROADMAP.md. Does not model a real time series: each query resolves to a single
 * survey year (the requested time range's end year, or the latest known year if none is given),
 * not a multi-year OpenAIRE-style range — but every returned data point still stamps a "period"
 * dimension for that resolved year (matching OpenAireBibliometricIndicatorQueryHandler's own
 * PERIOD_DIMENSION convention), because TimeSeriesNormalizer gap-fills any data point it can't
 * match against the requested range's generated periods — without this stamp it would treat every
 * real value as "missing" and zero it out whenever a caller supplies a timeSeries range.
 *
 * <p>oa.country_initiative_status (ATTRIBUTE) and oa.eu_country_coverage (RATIO) are two
 * renderings of the same per-initiativeType Yes/No dataset — one raw per-country map, one turned
 * into a percentage — so both share {@link #fetchInitiativeStatusByCountry} rather than issuing
 * duplicate stats-tool calls. oa.eu_country_coverage's denominator is that year's raw survey
 * respondent count (matching observatory-ui's own calculatePercentage exactly, including
 * countries that gave no answer), not a fixed EU-membership population — see
 * docs/INDICATOR_ROADMAP.md for why this isn't actually filtered to EU countries.
 */
@Service
public class SurveyStatsToolIndicatorQueryHandler implements IndicatorQueryHandler, RemoteIndicatorClient {

    public static final String HANDLER_KEY = "survey-stats-tool";

    private static final String COUNTRY_DIMENSION = IndicatorCodes.COUNTRY_DIMENSION;
    private static final String INITIATIVE_TYPE_DIMENSION = IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
    private static final String PERIOD_DIMENSION = "period";

    /**
     * Latest survey year confirmed live to carry all 4 initiativeType question codes and
     * Question56 — used when a query doesn't specify a time range. 2021 is deliberately excluded,
     * see the class Javadoc.
     */
    private static final int DEFAULT_YEAR = 2023;

    private static final String FINANCIAL_INVESTMENT_QUESTION_CODE = "Question56";

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
            default -> throw new IllegalStateException(
                "SurveyStatsToolIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
        };
    }

    /** One row per country that answered; country grouping is inherent to this indicator's meaning, not driven by plan.groupBy(). */
    private IndicatorResult queryCountryInitiativeStatus(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInitiativeType(requiredFilterValue(plan, INITIATIVE_TYPE_DIMENSION));
        InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(resolveYear(plan), questionCode);

        Map<String, Object> periodDimension = periodDimension(plan);
        List<IndicatorDataPoint> data = fetch.byCountry().entrySet().stream()
            .map(entry -> {
                Map<String, Object> dimensions = new LinkedHashMap<>();
                dimensions.put(COUNTRY_DIMENSION, entry.getKey());
                dimensions.putAll(periodDimension);
                return new IndicatorDataPoint(dimensions, entry.getValue());
            })
            .toList();

        List<String> dimensionKeys = new ArrayList<>(List.of(COUNTRY_DIMENSION));
        if (plan.timeRange() != null) {
            dimensionKeys.add(PERIOD_DIMENSION);
        }
        return buildResult(plan, dimensionKeys, data);
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
        int totalRespondents = fetch.totalRespondents();
        Object value = totalRespondents == 0 ? null : Math.round(adopted * 1000.0 / totalRespondents) / 10.0;

        List<String> dimensionKeys = plan.timeRange() != null ? List.of(PERIOD_DIMENSION) : List.of();
        return buildResult(plan, dimensionKeys, List.of(new IndicatorDataPoint(periodDimension(plan), value)));
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

    /** Applies the requested AggregationPolicy server-side over per-country euro values; survey values arrive in millions, scaled x1,000,000 here. */
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
            List<IndicatorDataPoint> data = millionsByCountry.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> dimensions = new LinkedHashMap<>();
                    dimensions.put(COUNTRY_DIMENSION, entry.getKey());
                    dimensions.putAll(periodDimension);
                    return new IndicatorDataPoint(dimensions, toEuros(entry.getValue()));
                })
                .toList();
            List<String> dimensionKeys = new ArrayList<>(List.of(COUNTRY_DIMENSION));
            if (plan.timeRange() != null) {
                dimensionKeys.add(PERIOD_DIMENSION);
            }
            return buildResult(plan, dimensionKeys, data);
        }

        List<String> requestedCountries = filterValues(plan, COUNTRY_DIMENSION);
        List<Double> millionsValues = requestedCountries.isEmpty()
            ? List.copyOf(millionsByCountry.values())
            : requestedCountries.stream().map(millionsByCountry::get).filter(Objects::nonNull).toList();

        Object aggregated = aggregate(plan.aggregation(), millionsValues);
        List<String> dimensionKeys = plan.timeRange() != null ? List.of(PERIOD_DIMENSION) : List.of();
        return buildResult(plan, dimensionKeys, List.of(new IndicatorDataPoint(periodDimension, aggregated)));
    }

    private Object aggregate(AggregationType aggregation, List<Double> millionsValues) {
        if (millionsValues.isEmpty()) {
            return null;
        }
        double resultInMillions = switch (aggregation) {
            case SUM -> millionsValues.stream().mapToDouble(Double::doubleValue).sum();
            case AVG -> millionsValues.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
            case MIN -> millionsValues.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
            case MAX -> millionsValues.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
            default -> throw new IllegalStateException(
                "Unsupported aggregation for oa.financial_investment: " + aggregation);
        };
        return toEuros(resultInMillions);
    }

    /** Survey values are collected in millions of euros; the indicator's declared unit is plain currency (euros). */
    private static double toEuros(double millions) {
        return millions * 1_000_000.0;
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

    private String requiredFilterValue(IndicatorExecutionPlan plan, String dimensionCode) {
        return plan.filters().stream()
            .filter(filter -> filter.dimensionCode().equals(dimensionCode))
            .findFirst()
            .map(filter -> String.valueOf(filter.values().get(0)))
            .orElseThrow(() -> new IllegalStateException(
                "Expected a required '" + dimensionCode + "' filter to already have been validated"));
    }

    /** Every value of every filter on the given dimension — empty if none, e.g. for an optional country filter. */
    private List<String> filterValues(IndicatorExecutionPlan plan, String dimensionCode) {
        return plan.filters().stream()
            .filter(filter -> filter.dimensionCode().equals(dimensionCode))
            .flatMap(filter -> filter.values().stream())
            .map(String::valueOf)
            .toList();
    }

    /** No timeRange -> the latest known survey year; otherwise the requested range's end year (single-year, not a real series). */
    private int resolveYear(IndicatorExecutionPlan plan) {
        return plan.timeRange() != null ? plan.timeRange().to().getYear() : DEFAULT_YEAR;
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

    private IndicatorResult buildResult(IndicatorExecutionPlan plan, List<String> dimensionKeys, List<IndicatorDataPoint> data) {
        IndicatorMetadata metadata = new IndicatorMetadata(
            plan.definition().code(), plan.definition().label(), plan.definition().valueType(),
            plan.definition().unit(), plan.aggregation()
        );
        return new IndicatorResult(metadata, dimensionKeys, data, new QueryExecutionMetadata(Instant.now(), false));
    }
}
