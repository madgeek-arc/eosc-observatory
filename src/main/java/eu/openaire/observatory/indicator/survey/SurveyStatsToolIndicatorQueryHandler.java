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
    private static final String SOFTWARE_OPEN_SOURCE_SETS_QUESTION_CODE = "Question73";

    /** The 3 values a country_initiative_status data point can carry — see IndicatorDefinitionCatalog.countryInitiativeStatus(). */
    public static final String STATUS_YES = "YES";
    public static final String STATUS_NO = "NO";
    public static final String STATUS_AWAITING_DATA = "AWAITING_DATA";

    /** The 4 values a country_policy_status data point can carry — see IndicatorDefinitionCatalog.countryPolicyStatus(). */
    public static final String POLICY_STATUS_MANDATORY = "MANDATORY";
    public static final String POLICY_STATUS_NON_MANDATORY = "NON_MANDATORY";
    public static final String POLICY_STATUS_NO_POLICY = "NO_POLICY";
    public static final String POLICY_STATUS_AWAITING_DATA = STATUS_AWAITING_DATA;

    /**
     * One survey question per fixed initiativeType member — confirmed live, cross-referenced
     * against observatory-ui's report-chart.configuration.ts (OA_PUBLICATION_POLICY/
     * IMMEDIATE_OA_POLICY/FINANCIAL_STRATEGY/MONITORING_INITIATIVE) and, for every other member,
     * against the real m-eosc-sb-2022/2023/2024/2025 registry survey models directly (same question
     * numbering confirmed stable across all 4 years) plus a live raw?json= call per code against both
     * the 2022 and 2023 survey years (the years this catalog actually serves). Every "*_POLICY"
     * member's mandatory sub-question is derivable as questionCode + ".1" (e.g. Question18 ->
     * Question18.1) — see {@link #mandatoryQuestionCode}, same live-verified.
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
        Map.entry("FAIR_DATA_MONITORING_INITIATIVE", "Question62"),
        Map.entry("DATA_MANAGEMENT_POLICY", "Question10"),
        Map.entry("DATA_MANAGEMENT_FINANCIAL_STRATEGY", "Question11"),
        Map.entry("DATA_MANAGEMENT_MONITORING_INITIATIVE", "Question58"),
        Map.entry("CITIZEN_SCIENCE_POLICY", "Question50"),
        Map.entry("CITIZEN_SCIENCE_FINANCIAL_STRATEGY", "Question51"),
        Map.entry("CITIZEN_SCIENCE_MONITORING_INITIATIVE", "Question98"),
        Map.entry("REPOSITORIES_CONNECT_POLICY", "Question30"),
        Map.entry("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY", "Question31"),
        Map.entry("REPOSITORIES_CONNECT_MONITORING_INITIATIVE", "Question78"),
        Map.entry("REPOSITORIES_PRESERVATION_POLICY", "Question38"),
        Map.entry("REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE", "Question86"),
        Map.entry("TRAINING_POLICY", "Question42"),
        Map.entry("TRAINING_FINANCIAL_STRATEGY", "Question43"),
        Map.entry("TRAINING_MONITORING_INITIATIVE", "Question90"),
        Map.entry("SOFTWARE_POLICY", "Question22"),
        Map.entry("SOFTWARE_FINANCIAL_STRATEGY", "Question23"),
        Map.entry("SOFTWARE_MONITORING_INITIATIVE", "Question70")
    );

    /**
     * One survey question per investmentArea member — a separate numeric ("in millions of Euros")
     * question series from QUESTION_CODE_BY_INITIATIVE_TYPE's Yes/No ones. Confirmed live against
     * the real m-eosc-sb-2022/2023/2024/2025 registry survey models (same numbering across all 4
     * years) plus a live raw?json= call per code against both the 2022 and 2023 survey years.
     */
    /**
     * The 9 real topic areas (independent of the initiativeType/investmentArea dimension's own
     * finer-grained naming, e.g. "IMMEDIATE_OA_POLICY" is a Publications-topic variant, not its own
     * topic area) — used only by {@link #queryAreasCount} to build each topic's "*_POLICY"/
     * "*_MONITORING_INITIATIVE" key without double-counting variants or picking up unrelated
     * "*_FINANCIAL_STRATEGY" keys via naive suffix matching.
     */
    private static final List<String> TOPIC_AREAS = List.of(
        "OA_PUBLICATION", "OPEN_DATA", "FAIR_DATA", "DATA_MANAGEMENT", "CITIZEN_SCIENCE",
        "REPOSITORIES_CONNECT", "REPOSITORIES_PRESERVATION", "TRAINING", "SOFTWARE"
    );

    private static final Map<String, String> QUESTION_CODE_BY_INVESTMENT_AREA = Map.ofEntries(
        Map.entry("OA_PUBLICATION", "Question56"),
        Map.entry("OPEN_DATA", "Question68"),
        Map.entry("FAIR_DATA", "Question64"),
        Map.entry("DATA_MANAGEMENT", "Question60"),
        Map.entry("CITIZEN_SCIENCE", "Question100"),
        Map.entry("REPOSITORIES_CONNECT", "Question80"),
        Map.entry("TRAINING", "Question92"),
        Map.entry("SOFTWARE", "Question72")
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
            case IndicatorCodes.COUNTRY_POLICY_STATUS -> queryCountryPolicyStatus(plan);
            case IndicatorCodes.MONITORED_AREAS_COUNT -> queryAreasCount(plan, "_MONITORING_INITIATIVE");
            case IndicatorCodes.POLICY_AREAS_COUNT -> queryAreasCount(plan, "_POLICY");
            case IndicatorCodes.LONG_TERM_PRESERVATION_COUNT -> queryCountByQuestion(plan, LONG_TERM_PRESERVATION_QUESTION_CODE);
            case IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT -> queryCountByQuestion(plan, CITIZEN_SCIENCE_NEW_PROJECTS_QUESTION_CODE);
            case IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT -> queryCountByQuestion(plan, SOFTWARE_OPEN_SOURCE_SETS_QUESTION_CODE);
            default -> throw new IllegalStateException(
                "SurveyStatsToolIndicatorQueryHandler has no logic for providerIndicatorKey: " + providerIndicatorKey);
        };
    }

    /**
     * One row per (country, initiativeType) that answered. Country grouping is always present,
     * inherent to this indicator's meaning; initiativeType additionally fans out into its own
     * dimension when the plan groups by it (e.g. the "National Monitoring/Policies of Open Science by
     * Countries and Areas" country x area matrix — a flat list of (country, initiativeType, status)
     * triples the caller pivots into a matrix, needing no dedicated matrix RenderHint). Otherwise
     * initiativeType must resolve to exactly one value (the original single-type snapshot shape).
     */
    private IndicatorResult queryCountryInitiativeStatus(IndicatorExecutionPlan plan) {
        List<String> types = filterValues(plan, INITIATIVE_TYPE_DIMENSION);
        boolean byType = groupByCodes(plan).contains(INITIATIVE_TYPE_DIMENSION);
        int year = resolveYear(plan);

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (String type : types) {
            InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(year, questionCodeForInitiativeType(type));
            narrowToRequestedCountries(plan, fetch.byCountry()).forEach((country, status) -> {
                Map<String, Object> dimensions = new LinkedHashMap<>();
                dimensions.put(COUNTRY_DIMENSION, country);
                if (byType) {
                    dimensions.put(INITIATIVE_TYPE_DIMENSION, type);
                }
                data.add(new IndicatorDataPoint(dimensions, status));
            });
        }
        List<String> dimensionKeys = byType
            ? List.of(COUNTRY_DIMENSION, INITIATIVE_TYPE_DIMENSION) : List.of(COUNTRY_DIMENSION);
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data);
    }

    /**
     * Denominator is that year's raw survey respondent count for this question (matching
     * observatory-ui's own calculatePercentage exactly) — a country with no answer still counts
     * toward the denominator, it's just not counted as adopting. An optional country filter narrows
     * both numerator and denominator to just the requested country/countries (respondents outside
     * the filter, if any); grouping by country instead returns one 0%/100% row per (scoped) country.
     * Supports a real multi-year trend (one point per year, like financialInvestment) and — like
     * queryCountryInitiativeStatus — fans out into one point per initiativeType when the plan groups
     * by it (e.g. "Trends in National Open Science Policies by Areas": one %-per-year series per
     * policy area, in a single query).
     */
    private IndicatorResult queryEuCountryCoverage(IndicatorExecutionPlan plan) {
        List<String> types = filterValues(plan, INITIATIVE_TYPE_DIMENSION);
        boolean byType = groupByCodes(plan).contains(INITIATIVE_TYPE_DIMENSION);
        boolean byCountry = groupByCodes(plan).contains(COUNTRY_DIMENSION);

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int year : resolveYears(plan)) {
            Map<String, Object> periodDimensions = periodDimensions(plan, year);
            for (String type : types) {
                InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(year, questionCodeForInitiativeType(type));
                Map<String, String> scoped = narrowToRequestedCountries(plan, fetch.byCountry());

                if (byCountry) {
                    scoped.forEach((country, status) -> {
                        Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                        dimensions.put(COUNTRY_DIMENSION, country);
                        if (byType) {
                            dimensions.put(INITIATIVE_TYPE_DIMENSION, type);
                        }
                        data.add(new IndicatorDataPoint(dimensions, RatioPercentage.of(STATUS_YES.equals(status) ? 1 : 0, 1)));
                    });
                } else {
                    long adopted = scoped.values().stream().filter(STATUS_YES::equals).count();
                    long total = filterValues(plan, COUNTRY_DIMENSION).isEmpty() ? fetch.totalRespondents() : scoped.size();
                    Object value = RatioPercentage.of(adopted, total);

                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    if (byType) {
                        dimensions.put(INITIATIVE_TYPE_DIMENSION, type);
                    }
                    data.add(new IndicatorDataPoint(dimensions, value));
                }
            }
        }
        List<String> dimensionKeys = new ArrayList<>();
        if (byType) {
            dimensionKeys.add(INITIATIVE_TYPE_DIMENSION);
        }
        if (byCountry) {
            dimensionKeys.add(COUNTRY_DIMENSION);
        }
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data);
    }

    /**
     * 4-state (MANDATORY/NON_MANDATORY/NO_POLICY/AWAITING_DATA) counterpart to
     * queryCountryInitiativeStatus, valid only for "*_POLICY" initiativeType members — combines two
     * survey answers per country: the base Yes/No policy question, and (only asked when the base
     * answer was Yes) a nested Yes/No "is this policy mandatory?" question at questionCode + ".1"
     * (see {@link #mandatoryQuestionCode}). A country that answered No to the base question is
     * NO_POLICY outright — the mandatory sub-question is skip-logic'd for them in the survey itself,
     * so a missing answer there does not mean AWAITING_DATA.
     */
    private IndicatorResult queryCountryPolicyStatus(IndicatorExecutionPlan plan) {
        String questionCode = questionCodeForInitiativeType(requiredFilterValue(plan, INITIATIVE_TYPE_DIMENSION));
        int year = resolveYear(plan);

        Map<String, String> baseByCountry = narrowToRequestedCountries(plan, fetchInitiativeStatusByCountry(year, questionCode).byCountry());
        Map<String, String> mandatoryByCountry = fetchInitiativeStatusByCountry(year, mandatoryQuestionCode(questionCode)).byCountry();

        List<IndicatorDataPoint> data = baseByCountry.entrySet().stream()
            .map(entry -> {
                String country = entry.getKey();
                String status = switch (entry.getValue()) {
                    case STATUS_NO -> POLICY_STATUS_NO_POLICY;
                    case STATUS_AWAITING_DATA -> POLICY_STATUS_AWAITING_DATA;
                    case STATUS_YES -> switch (mandatoryByCountry.getOrDefault(country, STATUS_AWAITING_DATA)) {
                        case STATUS_YES -> POLICY_STATUS_MANDATORY;
                        case STATUS_NO -> POLICY_STATUS_NON_MANDATORY;
                        default -> POLICY_STATUS_AWAITING_DATA;
                    };
                    default -> throw new IllegalStateException("Unexpected base policy status: " + entry.getValue());
                };
                return new IndicatorDataPoint(Map.of(COUNTRY_DIMENSION, country), status);
            })
            .toList();
        return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), data);
    }

    /**
     * Cross-topic composite for the "National Monitoring (number of monitored areas per country)" /
     * "National Policies (number of policy areas per country)" rows: for one country, how many of
     * the 9 real topic areas' "*_MONITORING_INITIATIVE" (or "*_POLICY") initiativeType member did it
     * answer Yes to — built from {@link #TOPIC_AREAS}, not a naive suffix scan over every
     * initiativeType member, since e.g. "IMMEDIATE_OA_POLICY" is a Publications-topic variant that
     * would otherwise double-count that one topic. memberSuffix selects Monitoring vs Policy —
     * mixing the two into one count wouldn't be a meaningful number. A country not answering a given
     * area's question simply doesn't contribute a Yes for that area (same "no answer" handling as
     * everywhere else in this handler) rather than being excluded from the result outright.
     */
    private IndicatorResult queryAreasCount(IndicatorExecutionPlan plan, String memberSuffix) {
        List<String> types = TOPIC_AREAS.stream()
            .map(topic -> areaMemberKey(topic, memberSuffix))
            .filter(QUESTION_CODE_BY_INITIATIVE_TYPE::containsKey)
            .toList();
        int year = resolveYear(plan);

        Map<String, Long> countByCountry = new LinkedHashMap<>();
        for (String type : types) {
            InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(year, questionCodeForInitiativeType(type));
            fetch.byCountry().forEach((country, status) -> {
                if (STATUS_YES.equals(status)) {
                    countByCountry.merge(country, 1L, Long::sum);
                } else {
                    countByCountry.putIfAbsent(country, 0L);
                }
            });
        }
        List<IndicatorDataPoint> data = byCountryDataPoints(narrowToRequestedCountries(plan, countByCountry));
        return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), data);
    }

    /**
     * Fetches the raw per-country Yes/No/no-answer rows once; shared by queryCountryInitiativeStatus
     * and queryEuCountryCoverage so the same initiativeType question is never fetched twice.
     * totalRespondents is the raw row count, which already includes AWAITING_DATA countries.
     * Non-respondent countries are reported as AWAITING_DATA rather than dropped, so a consumer can
     * tell "answered No" apart from "hasn't answered yet" — the survey itself has no richer status
     * than this 3-value set (confirmed live: every policy/monitoring question is a flat Yes/No radio
     * field, see IndicatorDefinitionCatalog.countryInitiativeStatus() javadoc).
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
     *
     * <p>investmentArea is optional: omitted, every area's investment is summed together first
     * (per country, then across countries per the requested aggregation) — "Total amount of
     * investments in EOSC and Open Science". Grouping by investmentArea instead fans out into one
     * point per area — "Investments in EOSC and Open Science (by Open Science area)". Both compose
     * with country grouping (area x country) the same way country grouping alone already did.
     */
    private IndicatorResult queryFinancialInvestment(IndicatorExecutionPlan plan) {
        List<String> areas = filterValues(plan, INVESTMENT_AREA_DIMENSION);
        if (areas.isEmpty()) {
            areas = List.copyOf(QUESTION_CODE_BY_INVESTMENT_AREA.keySet());
        }
        boolean byCountry = groupByCodes(plan).contains(COUNTRY_DIMENSION);
        boolean byArea = groupByCodes(plan).contains(INVESTMENT_AREA_DIMENSION);

        List<IndicatorDataPoint> data = new ArrayList<>();
        for (int year : resolveYears(plan)) {
            Map<String, Object> periodDimensions = periodDimensions(plan, year);

            Map<String, Map<String, Double>> millionsByCountryPerArea = new LinkedHashMap<>();
            Map<String, Double> millionsByCountryAllAreas = new LinkedHashMap<>();
            for (String area : areas) {
                Map<String, Double> millionsByCountry = fetchDoublesByCountry(year, questionCodeForInvestmentArea(area));
                millionsByCountryPerArea.put(area, millionsByCountry);
                millionsByCountry.forEach((country, millions) -> millionsByCountryAllAreas.merge(country, millions, Double::sum));
            }

            if (byArea) {
                for (String area : areas) {
                    Map<String, Double> millionsByCountry = millionsByCountryPerArea.get(area);
                    if (byCountry) {
                        millionsByCountry.forEach((country, millions) -> {
                            Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                            dimensions.put(INVESTMENT_AREA_DIMENSION, area);
                            dimensions.put(COUNTRY_DIMENSION, country);
                            data.add(new IndicatorDataPoint(dimensions, toEuros(millions)));
                        });
                    } else {
                        Object aggregated = aggregatedEuros(plan, millionsByCountry);
                        Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                        dimensions.put(INVESTMENT_AREA_DIMENSION, area);
                        data.add(new IndicatorDataPoint(dimensions, aggregated));
                    }
                }
            } else if (byCountry) {
                millionsByCountryAllAreas.forEach((country, millions) -> {
                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    dimensions.put(COUNTRY_DIMENSION, country);
                    data.add(new IndicatorDataPoint(dimensions, toEuros(millions)));
                });
            } else {
                data.add(new IndicatorDataPoint(periodDimensions, aggregatedEuros(plan, millionsByCountryAllAreas)));
            }
        }
        List<String> dimensionKeys = new ArrayList<>();
        if (byArea) {
            dimensionKeys.add(INVESTMENT_AREA_DIMENSION);
        }
        if (byCountry) {
            dimensionKeys.add(COUNTRY_DIMENSION);
        }
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data);
    }

    private Object aggregatedEuros(IndicatorExecutionPlan plan, Map<String, Double> millionsByCountry) {
        Double aggregatedMillions = (Double) aggregateNumbers(
            plan.aggregation(), selectedValues(plan, millionsByCountry), Double::sum, "oa.financial_investment");
        return aggregatedMillions == null ? null : toEuros(aggregatedMillions);
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

    /**
     * OA_PUBLICATION's monitoring member predates the topic-prefixed naming convention every other
     * topic uses ("MONITORING_INITIATIVE", not "OA_PUBLICATION_MONITORING_INITIATIVE") — kept as-is
     * rather than renamed, since that vocabulary is already live elsewhere (dimension search,
     * observatory-ui). This maps a topic + suffix to the initiativeType key that actually exists.
     */
    private static String areaMemberKey(String topic, String suffix) {
        if (topic.equals("OA_PUBLICATION") && suffix.equals("_MONITORING_INITIATIVE")) {
            return "MONITORING_INITIATIVE";
        }
        return topic + suffix;
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
     * Map-preserving counterpart to {@link #selectedValues}, for the per-country listing methods
     * (queryCountryInitiativeStatus/queryCountryPolicyStatus/queryAreasCount/queryEuCountryCoverage)
     * rather than the aggregate ones: narrows {@code byCountry} down to just the country filter's
     * values (dropping any requested country absent from the data), or returns it unchanged if no
     * country filter was given.
     */
    private static <V> Map<String, V> narrowToRequestedCountries(IndicatorExecutionPlan plan, Map<String, V> byCountry) {
        List<String> requestedCountries = filterValues(plan, COUNTRY_DIMENSION);
        if (requestedCountries.isEmpty()) {
            return byCountry;
        }
        Map<String, V> narrowed = new LinkedHashMap<>();
        for (String country : requestedCountries) {
            V value = byCountry.get(country);
            if (value != null) {
                narrowed.put(country, value);
            }
        }
        return narrowed;
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

    /**
     * A "*_POLICY" initiativeType member's nested "Is this policy mandatory?" sub-question is always
     * questionCode + ".1" (e.g. Question18 -> Question18.1, Question6 -> Question6.1) — confirmed
     * live for every "*_POLICY" member against both the 2022 and 2023 survey years. Only meaningful
     * for policy questions — callers are expected to only pass a "*_POLICY" member's questionCode.
     */
    private static String mandatoryQuestionCode(String policyQuestionCode) {
        return policyQuestionCode + ".1";
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
