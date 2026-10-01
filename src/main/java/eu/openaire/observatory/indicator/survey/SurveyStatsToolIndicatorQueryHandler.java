package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.dimension.OpenScienceArea;
import eu.openaire.observatory.indicator.handler.AbstractIndicatorQueryHandler;
import eu.openaire.observatory.indicator.handler.RatioPercentage;
import eu.openaire.observatory.indicator.handler.RemoteIndicatorClient;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorExclusion;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.IndicatorExecutionPlan;
import eu.openaire.observatory.indicator.validation.InvalidFilterShapeException;
import eu.openaire.observatory.indicator.validation.ResolvedDimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.IntStream;

/**
 * Real handler for the survey-stats-tool-backed indicators (oa.financial_investment,
 * oa.country_initiative_status, oa.eu_country_coverage, repositories.long_term_preservation_count,
 * citizen_science.new_projects_count), all sourced from a separate external survey stats-tool
 * (distinct from the OpenAIRE one — see indicator/openaire/). oa.financial_investment and
 * oa.country_initiative_status/oa.eu_country_coverage are cross-topic despite the "oa." prefix —
 * required investmentArea/initiativeType filters pick which topic's question is queried (see
 * QUESTION_CODE_BY_INVESTMENT_AREA/QUESTION_CODE_BY_INITIATIVE_TYPE below).
 *
 * <p>Any requested survey year is served. When the survey-stats-tool has no data for a given
 * {@code eosc.sb.<year>.<question>} — an unregistered named query, or (as for 2021, whose
 * "Question6"/"Question7" are unrelated numeric funding data and whose "Question54"/"Question6.3"/
 * "Question56" aren't registered at all) a value that can't be read as the expected Yes/No or
 * number — that country/period simply carries no answer, which composes into {@code null} values
 * rather than an error. Each missing {@code (year, question)} is logged once (WARN) per process.
 * A question paused in that year's survey model (see {@link PausedSurveyQuestions}) is treated the
 * same way: it is not fetched and carries no answer.
 *
 * <p>Every indicator here supports a real multi-year trend (TimePolicy(supported=true)): when the
 * plan carries a timeSeries range, {@link #resolveYears} fetches one point per year instead of
 * requiring a single surveyYear filter, mirroring OpenAireStatsToolIndicatorQueryHandler's own
 * period-per-year loop. Without a timeSeries range the (optional) surveyYear filter picks one year
 * for a snapshot; the validator rejects a query that carries neither as a 400 (there's no "latest
 * survey" concept to default to, same as surveyId elsewhere in this catalog).
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
    public static final String POLICY_STATUS_MANDATORY = "Mandatory policy";
    public static final String POLICY_STATUS_NON_MANDATORY = "Policy but not mandatory";
    public static final String POLICY_STATUS_NO_POLICY = "No policy";
    public static final String POLICY_STATUS_AWAITING_DATA = "Awaiting data";

    /**
     * One survey question per fixed initiativeType member. Every "*_POLICY" member's mandatory
     * sub-question is derivable as questionCode + ".1" (e.g. Question18 -> Question18.1) — see
     * {@link #mandatoryQuestionCode}.
     */
    private static final Map<String, String> QUESTION_CODE_BY_INITIATIVE_TYPE = OpenScienceArea.initiativeQuestionCodes();

    /** One numeric ("in millions of Euros") survey question per investmentArea member. */
    private static final Map<String, String> QUESTION_CODE_BY_INVESTMENT_AREA = OpenScienceArea.investmentQuestionCodes();

    private static final Logger logger = LoggerFactory.getLogger(SurveyStatsToolIndicatorQueryHandler.class);

    private final SurveyStatsToolQueryBuilder queryBuilder;
    private final SurveyStatsToolClient client;
    private final PausedSurveyQuestions pausedQuestions;

    /** {@code year|questionCode} pairs already logged as missing, so the WARN fires once per process. */
    private final Set<String> loggedMissingData = ConcurrentHashMap.newKeySet();

    /** {@code year|questionCode} pairs already logged as paused, so the INFO fires once per process. */
    private final Set<String> loggedPausedQuestions = ConcurrentHashMap.newKeySet();

    public SurveyStatsToolIndicatorQueryHandler(SurveyStatsToolQueryBuilder queryBuilder, SurveyStatsToolClient client,
                                                PausedSurveyQuestions pausedQuestions) {
        this.queryBuilder = queryBuilder;
        this.client = client;
        this.pausedQuestions = pausedQuestions;
    }

    /**
     * Fetches one question's per-country rows for a year, or empty when there is nothing to count: the
     * question is paused in that year's survey, or the stats-tool has no data for it.
     */
    private Optional<List<List<String>>> fetchQuestionRows(PausedSurveyQuestions.Snapshot paused, int year, String questionCode, String indicatorCode) {
        if (paused.isPaused(year, questionCode)) {
            if (loggedPausedQuestions.add(year + "|" + questionCode)) {
                logger.info("Skipping paused survey question {} for {} (indicator {})", questionCode, year, indicatorCode);
            }
            return Optional.empty();
        }
        Optional<List<List<String>>> maybeRows = client.fetchRowsIfAvailable(
            queryBuilder.surveyQuestionSeriesRequest(year, questionCode));
        if (maybeRows.isEmpty()) {
            warnNoData(year, questionCode, indicatorCode);
        }
        return maybeRows;
    }

    /** Whether the member's question is paused that year; if so, records the exclusion. */
    private boolean excludeIfPaused(PausedSurveyQuestions.Snapshot paused, List<IndicatorExclusion> exclusions, int year,
                                    String dimension, String member, String questionCode) {
        if (!paused.isPaused(year, questionCode)) {
            return false;
        }
        exclusions.add(IndicatorExclusion.questionPaused(year, dimension, member));
        return true;
    }

    private void warnNoData(int year, String questionCode, String indicatorCode) {
        if (loggedMissingData.add(year + "|" + questionCode)) {
            logger.warn("survey-stats-tool has no data for eosc.sb.{}.{} (indicator {})", year, questionCode, indicatorCode);
        }
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
            case IndicatorCodes.MONITORED_AREAS_COUNT -> queryAreasCount(plan, OpenScienceArea.InitiativeKind.MONITORING_INITIATIVE);
            case IndicatorCodes.POLICY_AREAS_COUNT -> queryAreasCount(plan, OpenScienceArea.InitiativeKind.POLICY);
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
     *
     * <p>Supports a real multi-year trend (one point per year, like {@link #queryEuCountryCoverage}):
     * a timeSeries range fetches one status per country per year; otherwise the single year comes
     * from the surveyYear filter.
     */
    private IndicatorResult queryCountryInitiativeStatus(IndicatorExecutionPlan plan) {
        List<String> types = filterValues(plan, INITIATIVE_TYPE_DIMENSION);
        boolean byType = groupByCodes(plan).contains(INITIATIVE_TYPE_DIMENSION);

        List<IndicatorDataPoint> data = new ArrayList<>();
        List<IndicatorExclusion> exclusions = new ArrayList<>();
        List<Integer> years = resolveYears(plan);
        PausedSurveyQuestions.Snapshot paused = pausedQuestions.snapshot(years);
        for (int year : years) {
            Map<String, Object> periodDimensions = periodDimensions(plan, year);
            for (String type : types) {
                excludeIfPaused(paused, exclusions, year, INITIATIVE_TYPE_DIMENSION, type, questionCodeForInitiativeType(type));
                InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(
                paused, year, questionCodeForInitiativeType(type), plan.definition().code());
                narrowToRequestedCountries(plan, fetch.byCountry(), STATUS_AWAITING_DATA).forEach((country, status) -> {
                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    dimensions.put(COUNTRY_DIMENSION, country);
                    if (byType) {
                        dimensions.put(INITIATIVE_TYPE_DIMENSION, type);
                    }
                    data.add(new IndicatorDataPoint(dimensions, status));
                });
            }
        }
        List<String> dimensionKeys = byType
            ? List.of(COUNTRY_DIMENSION, INITIATIVE_TYPE_DIMENSION) : List.of(COUNTRY_DIMENSION);
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data, exclusions);
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
        List<IndicatorExclusion> exclusions = new ArrayList<>();
        List<Integer> years = resolveYears(plan);
        PausedSurveyQuestions.Snapshot paused = pausedQuestions.snapshot(years);
        for (int year : years) {
            Map<String, Object> periodDimensions = periodDimensions(plan, year);
            for (String type : types) {
                excludeIfPaused(paused, exclusions, year, INITIATIVE_TYPE_DIMENSION, type, questionCodeForInitiativeType(type));
                InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(
                paused, year, questionCodeForInitiativeType(type), plan.definition().code());

                if (byCountry) {
                    // Per-country rows always include every requested country, but one with no
                    // usable answer (absent from the survey response, or STATUS_AWAITING_DATA)
                    // reports as null rather than a computed percentage — only an explicit Yes/No
                    // answer does; a country that explicitly answered No is a real 0%.
                    narrowToRequestedCountries(plan, fetch.byCountry()).forEach((country, status) -> {
                        Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                        dimensions.put(COUNTRY_DIMENSION, country);
                        if (byType) {
                            dimensions.put(INITIATIVE_TYPE_DIMENSION, type);
                        }
                        Object value = status == null || STATUS_AWAITING_DATA.equals(status)
                            ? null
                            : RatioPercentage.of(STATUS_YES.equals(status) ? 1 : 0, 1);
                        data.add(new IndicatorDataPoint(dimensions, value));
                    });
                } else {
                    // The aggregate percentage, unlike the per-country listing above, still excludes a
                    // requested-but-dataless country from both numerator and denominator (selectedValues)
                    // rather than counting it as a confirmed non-adopter — a different, correct-as-is
                    // concept (NullHandling.EXCLUDE) from "don't drop a row".
                    List<String> selected = selectedValues(plan, fetch.byCountry());
                    long adopted = selected.stream().filter(STATUS_YES::equals).count();
                    long total = filterValues(plan, COUNTRY_DIMENSION).isEmpty() ? fetch.totalRespondents() : selected.size();
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
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data, exclusions);
    }

    /**
     * 4-state ("Mandatory policy"/"Policy but not mandatory"/"No policy"/"Awaiting data") counterpart
     * to queryCountryInitiativeStatus, valid only for the 12 "<AREA>_POLICY" initiativeType members (any other member is rejected with a 400) — combines two
     * survey answers per country: the base Yes/No policy question, and (only asked when the base
     * answer was Yes) a nested Yes/No "is this policy mandatory?" question at questionCode + ".1"
     * (see {@link #mandatoryQuestionCode}). A country that answered No to the base question is
     * "No policy" outright — the mandatory sub-question is skip-logic'd for them in the survey
     * itself, so a missing answer there does not mean "Awaiting data".
     *
     * <p>Country grouping is always present, inherent to this indicator's meaning; initiativeType
     * additionally fans out into its own dimension when the plan groups by it (an IN filter + a
     * groupBy on initiativeType — the "National Open Science Policies by Countries and Areas"
     * country×area matrix), otherwise initiativeType is a single value (the original snapshot shape).
     */
    private IndicatorResult queryCountryPolicyStatus(IndicatorExecutionPlan plan) {
        List<String> types = filterValues(plan, INITIATIVE_TYPE_DIMENSION);
        requirePolicyTypes(types);
        boolean byType = groupByCodes(plan).contains(INITIATIVE_TYPE_DIMENSION);

        List<IndicatorDataPoint> data = new ArrayList<>();
        List<IndicatorExclusion> exclusions = new ArrayList<>();
        List<Integer> years = resolveYears(plan);
        PausedSurveyQuestions.Snapshot paused = pausedQuestions.snapshot(years);
        for (int year : years) {
            Map<String, Object> periodDimensions = periodDimensions(plan, year);
            for (String type : types) {
                String questionCode = questionCodeForInitiativeType(type);
                excludeIfPaused(paused, exclusions, year, INITIATIVE_TYPE_DIMENSION, type, questionCode);
                String indicatorCode = plan.definition().code();
                Map<String, String> baseByCountry = narrowToRequestedCountries(
                    plan, fetchInitiativeStatusByCountry(paused, year, questionCode, indicatorCode).byCountry(), STATUS_AWAITING_DATA);
                Map<String, String> mandatoryByCountry = fetchInitiativeStatusByCountry(
                paused, year, mandatoryQuestionCode(questionCode), indicatorCode).byCountry();

                baseByCountry.forEach((country, baseStatus) -> {
                    String status = switch (baseStatus) {
                        case STATUS_NO -> POLICY_STATUS_NO_POLICY;
                        case STATUS_AWAITING_DATA -> POLICY_STATUS_AWAITING_DATA;
                        case STATUS_YES -> switch (mandatoryByCountry.getOrDefault(country, STATUS_AWAITING_DATA)) {
                            case STATUS_YES -> POLICY_STATUS_MANDATORY;
                            case STATUS_NO -> POLICY_STATUS_NON_MANDATORY;
                            default -> POLICY_STATUS_AWAITING_DATA;
                        };
                        default -> throw new IllegalStateException("Unexpected base policy status: " + baseStatus);
                    };
                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    dimensions.put(COUNTRY_DIMENSION, country);
                    if (byType) {
                        dimensions.put(INITIATIVE_TYPE_DIMENSION, type);
                    }
                    data.add(new IndicatorDataPoint(dimensions, status));
                });
            }
        }
        List<String> dimensionKeys = byType
            ? List.of(COUNTRY_DIMENSION, INITIATIVE_TYPE_DIMENSION) : List.of(COUNTRY_DIMENSION);
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data, exclusions);
    }

    /** Rejects any initiativeType that isn't one of the 12 "<AREA>_POLICY" members: only those have a mandatory sub-question. */
    private static void requirePolicyTypes(List<String> types) {
        List<String> policyTypes = OpenScienceArea.initiativeTypes(OpenScienceArea.InitiativeKind.POLICY);
        for (String type : types) {
            if (!policyTypes.contains(type)) {
                throw new InvalidFilterShapeException(INITIATIVE_TYPE_DIMENSION,
                    "'%s' is not a policy initiative; expected one of %s".formatted(type, policyTypes));
            }
        }
    }

    /**
     * Cross-topic composite for the "National Monitoring (number of monitored areas per country)" /
     * "National Policies (number of policy areas per country)" rows: for one country, how many of
     * the {@link OpenScienceArea}s' monitoring (or policy) initiative did it answer Yes to. One
     * member per area is counted, so variants such as {@code IMMEDIATE_OA_POLICY} don't double-count
     * their area. {@code kind} selects Monitoring vs Policy — mixing the two into one count wouldn't
     * be a meaningful number. A country not answering a given area's question simply doesn't
     * contribute a Yes for that area (same "no answer" handling as everywhere else in this handler)
     * rather than being excluded from the result outright.
     */
    private IndicatorResult queryAreasCount(IndicatorExecutionPlan plan, OpenScienceArea.InitiativeKind kind) {
        List<String> types = OpenScienceArea.initiativeTypes(kind);

        List<IndicatorDataPoint> data = new ArrayList<>();
        List<IndicatorExclusion> exclusions = new ArrayList<>();
        List<Integer> years = resolveYears(plan);
        PausedSurveyQuestions.Snapshot paused = pausedQuestions.snapshot(years);
        for (int year : years) {
            Map<String, Long> countByCountry = new LinkedHashMap<>();
            int totalAreas = 0;
            for (String type : types) {
                if (!excludeIfPaused(paused, exclusions, year, INITIATIVE_TYPE_DIMENSION, type, questionCodeForInitiativeType(type))) {
                    totalAreas++;
                }
                InitiativeStatusFetch fetch = fetchInitiativeStatusByCountry(
                paused, year, questionCodeForInitiativeType(type), plan.definition().code());
                fetch.byCountry().forEach((country, status) -> {
                    if (STATUS_YES.equals(status)) {
                        countByCountry.merge(country, 1L, Long::sum);
                    } else {
                        countByCountry.putIfAbsent(country, 0L);
                    }
                });
            }
            Map<String, Object> periodDimensions = periodDimensions(plan, year);
            long total = totalAreas;
            narrowToRequestedCountries(plan, countByCountry).forEach((country, count) -> {
                Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                dimensions.put(COUNTRY_DIMENSION, country);
                data.add(new IndicatorDataPoint(dimensions, count, total));
            });
        }
        return buildResult(plan, dimensionKeysFor(List.of(COUNTRY_DIMENSION), plan), data, exclusions);
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
    private InitiativeStatusFetch fetchInitiativeStatusByCountry(PausedSurveyQuestions.Snapshot paused, int year, String questionCode, String indicatorCode) {
        Optional<List<List<String>>> maybeRows = fetchQuestionRows(paused, year, questionCode, indicatorCode);
        if (maybeRows.isEmpty()) {
            return new InitiativeStatusFetch(Map.of(), 0);
        }
        List<List<String>> rows = maybeRows.get();

        Map<String, String> byCountry = new LinkedHashMap<>();
        for (List<String> row : rows) {
            if (row.isEmpty()) {
                continue;
            }
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            byCountry.put(country, isNoAnswer(rawValue) ? STATUS_AWAITING_DATA : switch (rawValue) {
                case "Yes" -> STATUS_YES;
                case "No" -> STATUS_NO;
                // An unexpected value (e.g. 2021's Question6, which carries numeric funding data
                // where a Yes/No is expected) means "no usable answer", not a hard failure.
                default -> STATUS_AWAITING_DATA;
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
        List<IndicatorExclusion> exclusions = new ArrayList<>();
        List<Integer> years = resolveYears(plan);
        PausedSurveyQuestions.Snapshot paused = pausedQuestions.snapshot(years);
        for (int year : years) {
            Map<String, Object> periodDimensions = periodDimensions(plan, year);

            Map<String, Map<String, Double>> millionsByCountryPerArea = new LinkedHashMap<>();
            Map<String, Double> millionsByCountryAllAreas = new LinkedHashMap<>();
            for (String area : areas) {
                excludeIfPaused(paused, exclusions, year, INVESTMENT_AREA_DIMENSION, area, questionCodeForInvestmentArea(area));
                Map<String, Double> millionsByCountry = fetchDoublesByCountry(
                paused, year, questionCodeForInvestmentArea(area), plan.definition().code());
                millionsByCountryPerArea.put(area, millionsByCountry);
                millionsByCountry.forEach((country, millions) -> millionsByCountryAllAreas.merge(country, millions, Double::sum));
            }

            if (byArea) {
                for (String area : areas) {
                    Map<String, Double> millionsByCountry = millionsByCountryPerArea.get(area);
                    if (byCountry) {
                        narrowToRequestedCountries(plan, millionsByCountry).forEach((country, millions) -> {
                            Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                            dimensions.put(INVESTMENT_AREA_DIMENSION, area);
                            dimensions.put(COUNTRY_DIMENSION, country);
                            data.add(new IndicatorDataPoint(dimensions, millions == null ? null : toEuros(millions)));
                        });
                    } else {
                        Object aggregated = aggregatedEuros(plan, millionsByCountry);
                        Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                        dimensions.put(INVESTMENT_AREA_DIMENSION, area);
                        data.add(new IndicatorDataPoint(dimensions, aggregated));
                    }
                }
            } else if (byCountry) {
                narrowToRequestedCountries(plan, millionsByCountryAllAreas).forEach((country, millions) -> {
                    Map<String, Object> dimensions = new LinkedHashMap<>(periodDimensions);
                    dimensions.put(COUNTRY_DIMENSION, country);
                    data.add(new IndicatorDataPoint(dimensions, millions == null ? null : toEuros(millions)));
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
        return buildResult(plan, dimensionKeysFor(dimensionKeys, plan), data, exclusions);
    }

    private Object aggregatedEuros(IndicatorExecutionPlan plan, Map<String, Double> millionsByCountry) {
        Double aggregatedMillions = (Double) aggregateNumbers(
            plan.aggregation(), selectedValues(plan, millionsByCountry), Double::sum, "oa.financial_investment");
        return aggregatedMillions == null ? null : toEuros(aggregatedMillions);
    }

    private Map<String, Double> fetchDoublesByCountry(PausedSurveyQuestions.Snapshot paused, int year, String questionCode, String indicatorCode) {
        Optional<List<List<String>>> maybeRows = fetchQuestionRows(paused, year, questionCode, indicatorCode);
        if (maybeRows.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> byCountry = new LinkedHashMap<>();
        for (List<String> row : maybeRows.get()) {
            if (row.isEmpty()) {
                continue;
            }
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (isNoAnswer(rawValue)) {
                continue;
            }
            try {
                byCountry.put(country, Double.parseDouble(rawValue));
            } catch (NumberFormatException e) {
                // A non-numeric answer -> treat as no answer, same as an omitted country.
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
        List<IndicatorExclusion> exclusions = new ArrayList<>();
        List<Integer> years = resolveYears(plan);
        PausedSurveyQuestions.Snapshot paused = pausedQuestions.snapshot(years);
        for (int year : years) {
            if (paused.isPaused(year, questionCode)) {
                exclusions.add(IndicatorExclusion.questionPaused(year));
            }
            Map<String, Long> countByCountry = fetchCountsByCountry(paused, year, questionCode, plan.definition().code());
            Map<String, Object> periodDimensions = periodDimensions(plan, year);

            if (byCountry) {
                narrowToRequestedCountries(plan, countByCountry).forEach((country, value) -> {
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
        return buildResult(plan, dimensionKeysFor(byCountry ? List.of(COUNTRY_DIMENSION) : List.of(), plan), data, exclusions);
    }

    private Map<String, Long> fetchCountsByCountry(PausedSurveyQuestions.Snapshot paused, int year, String questionCode, String indicatorCode) {
        Optional<List<List<String>>> maybeRows = fetchQuestionRows(paused, year, questionCode, indicatorCode);
        if (maybeRows.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> byCountry = new LinkedHashMap<>();
        for (List<String> row : maybeRows.get()) {
            if (row.isEmpty()) {
                continue;
            }
            String country = row.get(0);
            String rawValue = row.size() > 1 ? row.get(1) : null;
            if (isNoAnswer(rawValue)) {
                continue;
            }
            try {
                byCountry.put(country, Math.round(Double.parseDouble(rawValue)));
            } catch (NumberFormatException e) {
                // A non-numeric answer -> treat as no answer, same as an omitted country.
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

    /** The requested countries' values (filtered to those with an answer), or every answering country's value if none were requested. */
    private <V> List<V> selectedValues(IndicatorExecutionPlan plan, Map<String, V> valueByCountry) {
        List<String> requestedCountries = filterValues(plan, COUNTRY_DIMENSION);
        return requestedCountries.isEmpty()
            ? List.copyOf(valueByCountry.values())
            : requestedCountries.stream().map(valueByCountry::get).filter(Objects::nonNull).toList();
    }

    /**
     * Map-preserving counterpart to {@link #selectedValues}, for the per-country listing methods
     * (queryCountryInitiativeStatus/queryCountryPolicyStatus/queryAreasCount/queryEuCountryCoverage/
     * queryFinancialInvestment/queryCountByQuestion, on their byCountry branches) rather than the
     * aggregate ones: every requested country gets a row, in request order. One present in
     * {@code byCountry} keeps its value; one absent gets {@code null} (the same "no data" signal
     * already used elsewhere in this indicator family for a missing period) — never dropped. Returns
     * {@code byCountry} unchanged if no country filter was given.
     */
    private static <V> Map<String, V> narrowToRequestedCountries(IndicatorExecutionPlan plan, Map<String, V> byCountry) {
        return narrowToRequestedCountries(plan, byCountry, null);
    }

    /**
     * Overload of {@link #narrowToRequestedCountries(IndicatorExecutionPlan, Map)} for a value type
     * with a defined "no usable answer" sentinel (e.g. {@link #STATUS_AWAITING_DATA}) instead of a
     * raw {@code null} — used where downstream code expects one of a fixed set of non-null values.
     */
    private static <V> Map<String, V> narrowToRequestedCountries(
            IndicatorExecutionPlan plan, Map<String, V> byCountry, V missingValue) {
        List<String> requestedCountries = filterValues(plan, COUNTRY_DIMENSION);
        if (requestedCountries.isEmpty()) {
            return byCountry;
        }
        Map<String, V> narrowed = new LinkedHashMap<>();
        for (String country : requestedCountries) {
            narrowed.put(country, byCountry.getOrDefault(country, missingValue));
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

    /**
     * Every year in the requested timeSeries range, or the single year picked by an explicit
     * surveyYear filter otherwise — there's no "latest survey" concept to default to when neither is
     * given (the validator rejects that combination as a 400 before we get here). Every query method
     * in this handler now goes through here: the per-country status/count indicators
     * (oa.country_initiative_status, oa.country_policy_status, oa.monitored_areas_count,
     * oa.policy_areas_count) resolve one year in the snapshot case and loop the range in the trend
     * case, exactly like oa.financial_investment / oa.eu_country_coverage / the count-by-question
     * indicators.
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
