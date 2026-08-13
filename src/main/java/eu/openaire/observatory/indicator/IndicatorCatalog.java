package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.model.DimensionType;
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
import eu.openaire.observatory.indicator.model.TemporalBehavior;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.model.ZeroDenominatorHandling;
import eu.openaire.observatory.indicator.local.LocalRegistryIndicatorQueryHandler;
import eu.openaire.observatory.indicator.openaire.OpenAireStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static eu.openaire.observatory.indicator.IndicatorCodes.ACCESS_STATUS_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_INITIATIVE_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.EU_COUNTRY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.FINANCIAL_INVESTMENT;
import static eu.openaire.observatory.indicator.IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.LONG_TERM_PRESERVATION_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.SURVEY_ID_DIMENSION;

/**
 * In-memory catalog of real indicator definitions. Not a stand-in for a future persistence layer
 * — the 9 definitions it holds today are all real: 2 route to OpenAireStatsToolIndicatorQueryHandler
 * (OpenAIRE data), 5 route to SurveyStatsToolIndicatorQueryHandler (survey stats-tool
 * data, scoped to survey years 2022+ — see that handler's Javadoc and docs/INDICATOR_ROADMAP.md
 * for why 2021 is excluded), and 2 route to LocalRegistryIndicatorQueryHandler (no external HTTP
 * call at all — wraps SurveyService/StakeholderService directly). It previously also held 3
 * synthetic mock indicators with the same codes as the 3 original survey ones; those were removed
 * and later replaced with real implementations — see docs/INDICATOR_ROADMAP.md for the design
 * history and remaining backlog beyond today's 9.
 */
@Service
public class IndicatorCatalog {

    private static final String GROUP_LABEL = "Publications";

    private static final TimePolicy TIME_POLICY = new TimePolicy(
        true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR,
        TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
    );

    /** For the 2 local-registry indicators, which have no year/time concept at all. */
    private static final TimePolicy NON_TIME_SERIES_POLICY = new TimePolicy(
        false, null, Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.OMIT
    );

    /** For MEASURE indicators summable across countries — publications.count and the 3 survey counts. */
    private static final AggregationPolicy SUM_AGGREGATION_POLICY = new AggregationPolicy(
        AggregationType.SUM,
        Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
        NullHandling.EXCLUDE
    );

    /** For RATIO/ATTRIBUTE indicators and the local-registry counts, which are never aggregated. */
    private static final AggregationPolicy NO_AGGREGATION_POLICY =
        new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE);

    private final Map<String, IndicatorDefinition> definitionsByCode;
    private final Map<String, DimensionDefinition> dimensionsByCode;

    public IndicatorCatalog() {
        DimensionDefinition countryDimension = new DimensionDefinition(
            COUNTRY_DIMENSION, "Country", DimensionType.ENTITY_REFERENCE,
            true, true, true, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition accessStatusDimension = new DimensionDefinition(
            ACCESS_STATUS_DIMENSION, "Access status", DimensionType.ENUM,
            true, true, false,
            Set.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.NOT_EQ, FilterOperator.NOT_IN), null
        );
        DimensionDefinition initiativeTypeDimension = new DimensionDefinition(
            INITIATIVE_TYPE_DIMENSION, "Initiative type", DimensionType.ENUM,
            true, true, false, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition surveyIdDimension = new DimensionDefinition(
            SURVEY_ID_DIMENSION, "Survey", DimensionType.STRING,
            true, false, false, Set.of(FilterOperator.EQ), null
        );
        this.dimensionsByCode = Map.of(
            COUNTRY_DIMENSION, countryDimension,
            ACCESS_STATUS_DIMENSION, accessStatusDimension,
            INITIATIVE_TYPE_DIMENSION, initiativeTypeDimension,
            SURVEY_ID_DIMENSION, surveyIdDimension
        );

        Map<String, IndicatorDefinition> byCode = new LinkedHashMap<>();
        for (IndicatorDefinition definition : allDefinitions()) {
            byCode.put(definition.code(), definition);
        }
        this.definitionsByCode = Map.copyOf(byCode);
    }

    public List<IndicatorDefinition> getAll() {
        return List.copyOf(definitionsByCode.values());
    }

    public IndicatorDefinition get(String code) {
        return definitionsByCode.get(code);
    }

    public DimensionDefinition dimension(String code) {
        return dimensionsByCode.get(code);
    }

    private static List<IndicatorDefinition> allDefinitions() {
        return List.of(
            publicationsCount(), publicationsAccessStatusShare(),
            financialInvestment(), countryInitiativeStatus(), euCountryCoverage(),
            longTermPreservationCount(), citizenScienceNewProjectsCount(),
            countriesWithValidatedAnswers(), stakeholderParticipatingCountriesCount()
        );
    }

    /**
     * Base publication counts, filterable/groupable by accessStatus (OA vs. Closed/Embargoed/Restricted)
     * and by a single 2-letter country code (filter narrows to one country; group fans out into one row
     * per country). Country absent means all-EU (the handler always scopes to continent=Europe).
     */
    private static IndicatorDefinition publicationsCount() {
        return definition(
            PUBLICATIONS_COUNT, "Publications",
            "Number of published works, filterable/groupable by a single access status value (OA, "
                + "Closed, Embargoed, or Restricted — never aggregated, unlike "
                + "publications.access_status_share) and by country.",
            GROUP_LABEL, IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ), false
                ),
                countryBinding()
            ),
            TIME_POLICY, OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy id 67 — "OA Publication in Europe (OA vs Closed)". Defaults to Europe-wide, but a
     * country filter narrows to one country's share, and grouping by country fans out into one row
     * per country (for a map). accessStatus is required: EQ + one value returns that exact status's
     * share; IN or NOT_EQ aggregate several statuses together (e.g. NOT_EQ OA for the full non-open
     * share).
     */
    private static IndicatorDefinition publicationsAccessStatusShare() {
        return definition(
            PUBLICATIONS_ACCESS_STATUS_SHARE, "Publications by Access Status (Share)",
            "Percentage share of publications matching the given access status filter, out of all "
                + "publications. A single EQ value returns that exact status's share (e.g. OA, or just "
                + "Embargoed); NOT_EQ or a multi-value IN aggregates several statuses together (e.g. "
                + "NOT_EQ OA for the full non-open share). The accessStatus filter is required — "
                + "there is no meaningful value without specifying which status(es) to measure as a "
                + "share of the total.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER),
                    Set.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.NOT_EQ, FilterOperator.NOT_IN), true
                ),
                countryBinding()
            ),
            TIME_POLICY, OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy id 69 — "Financial investments in OA Publication". Survey Question56, in millions of
     * euros; SurveyStatsToolIndicatorQueryHandler applies the ×1,000,000 scale so the returned
     * value is in euros. Filterable/groupable by country (absent country filter aggregates all
     * countries with an answer, per the requested AggregationPolicy — SUM by default).
     */
    private static IndicatorDefinition financialInvestment() {
        return definition(
            FINANCIAL_INVESTMENT, "Financial Investment in OA Publication",
            "Reported financial investment in Open Access publication (survey Question56), in euros. "
                + "Survey values are collected in millions of euros — this indicator applies an "
                + "explicit x1,000,000 scale before returning a value, so the returned number is "
                + "already in euros, not millions.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding()),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy ids 73/74 — "National policy"/"National monitoring on OA Publication", collapsed
     * behind a required initiativeType filter (fixed 4-value enum — see
     * InitiativeTypeDimensionMemberProvider). Per-country Yes/No, always one row per answering
     * country — country grouping is mandatory for this indicator to render, not optional, so
     * there is no country dimension binding at all (unlike the MEASURE/RATIO indicators above).
     */
    private static IndicatorDefinition countryInitiativeStatus() {
        return definition(
            COUNTRY_INITIATIVE_STATUS, "Country Initiative Status",
            "Per-country adoption status (Yes/No) of a national OA initiative, for a given "
                + "initiativeType (survey Question6/Question6.3/Question7/Question54, one per "
                + "initiativeType value). No aggregation — always one boolean per answering country.",
            GROUP_LABEL, IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, RenderHint.ENTITY_MAP,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(requiredEqFilterBinding(INITIATIVE_TYPE_DIMENSION)),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy ids 68/70/71/72 — "EU Countries with Policy/Immediate-OA-Policy/Financial-Strategy/
     * Monitoring on OA Publication", collapsed behind a required initiativeType filter. Two views
     * over the same per-country fact as countryInitiativeStatus() — the handler shares one
     * data-access call between them. Intrinsically EU-aggregate-only: no country binding, unlike
     * publications.access_status_share.
     */
    private static IndicatorDefinition euCountryCoverage() {
        return definition(
            EU_COUNTRY_COVERAGE, "EU Country Coverage",
            "Percentage of that year's survey respondents with a Yes on the given initiativeType's "
                + "national initiative question, out of all respondents to that question that year "
                + "(matches observatory-ui's own calculation exactly, including any non-EU "
                + "respondents present in the survey). A country with no answer to this specific "
                + "question still counts toward the denominator, just not as a Yes.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(requiredEqFilterBinding(INITIATIVE_TYPE_DIMENSION)),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Survey Question89: "How many repositories offered long-term data preservation in your
     * country?" — plain per-country integer count, structurally identical to financialInvestment()
     * apart from having no unit scale. Question code confirmed live against the real survey
     * stats-tool (see docs/INDICATOR_ROADMAP.md).
     */
    private static IndicatorDefinition longTermPreservationCount() {
        return definition(
            LONG_TERM_PRESERVATION_COUNT, "Repositories with Long-Term Preservation",
            "Number of repositories offering long-term data preservation, per country (survey "
                + "Question89). Filterable/groupable by country (absent country filter aggregates "
                + "all countries with an answer, per the requested AggregationPolicy — SUM by default).",
            "Data Preservation", IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding()),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Survey Question101: "How many projects with a citizen science dimension were started in
     * your country?" — plain per-country integer count, same shape as longTermPreservationCount().
     * Question code confirmed live against the real survey stats-tool (see
     * docs/INDICATOR_ROADMAP.md).
     */
    private static IndicatorDefinition citizenScienceNewProjectsCount() {
        return definition(
            CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, "New Citizen Science Projects",
            "Number of new projects with a citizen science dimension started, per country (survey "
                + "Question101). Filterable/groupable by country (absent country filter aggregates "
                + "all countries with an answer, per the requested AggregationPolicy — SUM by default).",
            "Citizen Science", IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding()),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Local-only — no external HTTP call. Backed directly by
     * SurveyServiceImpl.getCountriesWithValidatedAnswer, deduplicated (that method returns one
     * entry per validated answer, not per distinct country). surveyId is a required filter since
     * there's no "latest survey" concept to default to.
     */
    private static IndicatorDefinition countriesWithValidatedAnswers() {
        return definition(
            COUNTRIES_WITH_VALIDATED_ANSWERS, "Countries with Validated Survey Answers",
            "Number of distinct countries with at least one validated answer for the given survey. "
                + "The surveyId filter is required — there is no meaningful value without "
                + "specifying which survey.",
            "Survey", IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(requiredEqFilterBinding(SURVEY_ID_DIMENSION)),
            NON_TIME_SERIES_POLICY, LocalRegistryIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Local-only — no external HTTP call. Backed directly by Stakeholder records of type
     * "eosc-sb" (the EOSC Steering Board country representatives) — hardcoded, not a
     * client-filterable dimension, since this indicator is specifically about EOSC Steering Board
     * country participation, not stakeholder types in general.
     */
    private static IndicatorDefinition stakeholderParticipatingCountriesCount() {
        return definition(
            STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT, "Participating Countries",
            "Number of distinct countries with an EOSC Steering Board stakeholder registered.",
            "Stakeholders", IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(),
            NON_TIME_SERIES_POLICY, LocalRegistryIndicatorQueryHandler.HANDLER_KEY
        );
    }

    private static IndicatorDimensionBinding countryBinding() {
        return new IndicatorDimensionBinding(
            COUNTRY_DIMENSION,
            Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
            Set.of(FilterOperator.EQ, FilterOperator.IN),
            false
        );
    }

    /** A single required EQ-only filter dimension — no group usage, no other operators. Used by initiativeType and surveyId. */
    private static IndicatorDimensionBinding requiredEqFilterBinding(String dimensionCode) {
        return new IndicatorDimensionBinding(
            dimensionCode, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
        );
    }

    /**
     * Fills in the boilerplate every definition shares verbatim: a fresh id, a providerIndicatorKey
     * always equal to the indicator's own code, and the fixed PUBLIC/ACTIVE/version-1 trailer.
     */
    private static IndicatorDefinition definition(
            String code, String label, String description, String groupLabel,
            IndicatorValueType valueType, IndicatorSemanticType semanticType, UnitType unit, RenderHint renderHint,
            AggregationPolicy aggregationPolicy, RatioPolicy ratioPolicy,
            Set<IndicatorDimensionBinding> dimensionBindings, TimePolicy timePolicy, String handlerKey) {
        return new IndicatorDefinition(
            UUID.randomUUID(), code, label, description, groupLabel,
            valueType, semanticType, unit, renderHint,
            aggregationPolicy, ratioPolicy, dimensionBindings, timePolicy,
            new IndicatorExecutionBinding(handlerKey, code),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
