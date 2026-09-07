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
import static eu.openaire.observatory.indicator.IndicatorCodes.CLASSIFICATION_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_INITIATIVE_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_POLICY_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.DATASETS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.DATASETS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.EU_COUNTRY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.FINANCIAL_INVESTMENT;
import static eu.openaire.observatory.indicator.IndicatorCodes.GOLD_OA_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.GREEN_OA_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.INVESTMENT_AREA_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.LONG_TERM_PRESERVATION_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.MONITORED_AREAS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.OA_LICENSE_STATUS_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.POLICY_AREAS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.RESEARCHER_POLICY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.SOFTWARE_COUNT;
import static eu.openaire.observatory.indicator.IndicatorCodes.SURVEY_ID_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.SURVEY_YEAR_DIMENSION;

/**
 * In-memory catalog of real indicator definitions. Not a stand-in for a future persistence layer
 * — the 17 definitions it holds today are all real: 6 route to OpenAireStatsToolIndicatorQueryHandler
 * (OpenAIRE data — publications.oa_license_status_count is the europe_monitor-profiled outlier of
 * the 6, the other 5 use the default openaire_stats profile), 9 route to
 * SurveyStatsToolIndicatorQueryHandler (survey stats-tool data, scoped to survey years 2022+ — see
 * that handler's Javadoc for why 2021 is excluded), and 2 route to
 * LocalRegistryIndicatorQueryHandler (no external HTTP call at all — wraps
 * SurveyService/StakeholderService directly). It previously also held 3 synthetic mock indicators
 * with the same codes as the 3 original survey ones; those were removed and later replaced with
 * real implementations.
 */
@Service
public class IndicatorDefinitionCatalog {

    private static final TimePolicy TIME_POLICY = new TimePolicy(true, MissingPeriodHandling.ZERO_FILL);

    /** For the 2 local-registry indicators, which have no year/time concept at all. */
    private static final TimePolicy NON_TIME_SERIES_POLICY = new TimePolicy(false, MissingPeriodHandling.OMIT);

    /** For MEASURE indicators summable across countries — publications.count and the 3 survey counts. */
    private static final AggregationPolicy SUM_AGGREGATION_POLICY = new AggregationPolicy(
        AggregationType.SUM,
        Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MEDIAN, AggregationType.MIN, AggregationType.MAX),
        NullHandling.EXCLUDE
    );

    /** For RATIO/ATTRIBUTE indicators and the local-registry counts, which are never aggregated. */
    private static final AggregationPolicy NO_AGGREGATION_POLICY =
        new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE);

    private final Map<String, IndicatorDefinition> definitionsByCode;
    private final Map<String, DimensionDefinition> dimensionsByCode;

    public IndicatorDefinitionCatalog() {
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
        DimensionDefinition investmentAreaDimension = new DimensionDefinition(
            INVESTMENT_AREA_DIMENSION, "Investment area", DimensionType.ENUM,
            true, true, false, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition surveyIdDimension = new DimensionDefinition(
            SURVEY_ID_DIMENSION, "Survey", DimensionType.STRING,
            true, false, false, Set.of(FilterOperator.EQ), null
        );
        DimensionDefinition surveyYearDimension = new DimensionDefinition(
            SURVEY_YEAR_DIMENSION, "Survey Year", DimensionType.INTEGER,
            true, false, false, Set.of(FilterOperator.EQ), null
        );
        DimensionDefinition classificationDimension = new DimensionDefinition(
            CLASSIFICATION_DIMENSION, "Classification", DimensionType.STRING,
            true, true, false, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition fieldOfScienceDimension = new DimensionDefinition(
            FIELD_OF_SCIENCE_DIMENSION, "Field of science", DimensionType.STRING,
            true, true, false, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition goldOaDimension = new DimensionDefinition(
            GOLD_OA_DIMENSION, "Gold OA", DimensionType.ENUM,
            true, false, false, Set.of(FilterOperator.EQ), null
        );
        DimensionDefinition greenOaDimension = new DimensionDefinition(
            GREEN_OA_DIMENSION, "Green OA", DimensionType.ENUM,
            true, false, false, Set.of(FilterOperator.EQ), null
        );
        DimensionDefinition oaLicenseStatusDimension = new DimensionDefinition(
            OA_LICENSE_STATUS_DIMENSION, "OA license status", DimensionType.ENUM,
            true, true, false, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        this.dimensionsByCode = Map.ofEntries(
            Map.entry(COUNTRY_DIMENSION, countryDimension),
            Map.entry(ACCESS_STATUS_DIMENSION, accessStatusDimension),
            Map.entry(INITIATIVE_TYPE_DIMENSION, initiativeTypeDimension),
            Map.entry(INVESTMENT_AREA_DIMENSION, investmentAreaDimension),
            Map.entry(SURVEY_ID_DIMENSION, surveyIdDimension),
            Map.entry(SURVEY_YEAR_DIMENSION, surveyYearDimension),
            Map.entry(CLASSIFICATION_DIMENSION, classificationDimension),
            Map.entry(FIELD_OF_SCIENCE_DIMENSION, fieldOfScienceDimension),
            Map.entry(GOLD_OA_DIMENSION, goldOaDimension),
            Map.entry(GREEN_OA_DIMENSION, greenOaDimension),
            Map.entry(OA_LICENSE_STATUS_DIMENSION, oaLicenseStatusDimension)
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
            publicationsCount(), publicationsAccessStatusShare(), datasetsCount(), softwareCount(),
            publicationsOaLicenseStatusCount(), datasetsAccessStatusShare(),
            financialInvestment(), countryInitiativeStatus(), euCountryCoverage(), countryPolicyStatus(),
            monitoredAreasCount(), policyAreasCount(), researcherPolicyCoverage(),
            longTermPreservationCount(), citizenScienceNewProjectsCount(), softwareOpenSourceSetsCount(),
            countriesWithValidatedAnswers(), stakeholderParticipatingCountriesCount()
        );
    }

    /**
     * Base publication counts, filterable/groupable by accessStatus (OA vs. Closed/Embargoed/Restricted)
     * and by a single 2-letter country code (filter narrows to one country; group fans out into one row
     * per country). Country absent means all-EU (the handler always scopes to continent=Europe).
     * Also filterable/groupable by classification (document/output type, e.g. "Article") and
     * fieldOfScience (e.g. "01 natural sciences") — both use the same OpenAIRE field for filtering
     * and grouping (unlike country, which has separate filter/group fields). Also filter-only
     * (no groupBy) by goldOA and greenOA — two independent YES/NO flags (e.g. both goldOA=YES and
     * greenOA=YES together for "gold and green"), typically combined with a fieldOfScience groupBy.
     */
    private static IndicatorDefinition publicationsCount() {
        return definition(
            PUBLICATIONS_COUNT, "Publications",
            "Number of published works, filterable/groupable by a single access status value (OA, "
                + "Closed, Embargoed, or Restricted — never aggregated, unlike "
                + "publications.access_status_share), by country, by classification, or by "
                + "fieldOfScience. Also filterable (not groupable) by goldOA/greenOA, two "
                + "independent YES/NO flags.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ), false
                ),
                countryBinding(),
                filterableGroupableBinding(CLASSIFICATION_DIMENSION),
                filterableGroupableBinding(FIELD_OF_SCIENCE_DIMENSION),
                filterOnlyBinding(GOLD_OA_DIMENSION),
                filterOnlyBinding(GREEN_OA_DIMENSION)
            ),
            TIME_POLICY, OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy chart "OA Publication in Europe by License Status" (distributionOfOAPublications) —
     * the europe_monitor-profiled outlier among the OpenAIRE-routed indicators (every other one
     * uses openaire_stats). oaLicenseStatus is an optional, exhaustive 5-value partition
     * (OA_WITH_LICENSE/OA_WITHOUT_LICENSE/EMBARGOED/RESTRICTED/CLOSED): an EQ/IN filter picks the
     * status(es), grouping by it fans out one row per status (the "distribution of access types by
     * output type" preset), and with neither a single unfiltered total per classification is
     * returned. Always broken down by classification (its only mandatory groupable binding —
     * narrowed server-side to a fixed article/conference/chapter/book vocabulary, not
     * client-controlled). Also filterable/groupable by country (one or more countries OR'd together;
     * absent means all-Europe, matching the always-applied continent=Europe scope) — confirmed live:
     * the europe_monitor profile accepts a genuine second simultaneous select-group field
     * (classification + country), returning {@code [count, classification, country]} rows, not just
     * the single-group {@code [count, classification]} shape used elsewhere.
     */
    private static IndicatorDefinition publicationsOaLicenseStatusCount() {
        return definition(
            PUBLICATIONS_OA_LICENSE_STATUS_COUNT, "Publications by OA License Status",
            "Number of published works matching the given OA license status (OA with licence, OA "
                + "without licence, Embargoed, Restricted, or Closed), broken down by classification, "
                + "optionally also by country. The oaLicenseStatus filter is optional: an EQ/IN value "
                + "picks the status(es), grouping by oaLicenseStatus fans out one row per status, and "
                + "with neither a single unfiltered total per classification is returned. Routed to "
                + "the europe_monitor stats-tool profile, unlike every other OpenAIRE-routed "
                + "indicator in this catalog.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    OA_LICENSE_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN), false
                ),
                new IndicatorDimensionBinding(CLASSIFICATION_DIMENSION, Set.of(DimensionUsage.GROUP), Set.of(), false),
                countryBinding()
            ),
            TIME_POLICY, OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Dataset-entity counterpart to publicationsCount() — same shape and upstream
     * (OpenAireStatsToolIndicatorQueryHandler, scoped to continent=Europe), just entity=dataset
     * instead of entity=publication. Also filterable/groupable by classification (e.g. "Dataset",
     * "Collection") — fieldOfScience is publication-only, not evidenced for datasets in the legacy UI.
     */
    private static IndicatorDefinition datasetsCount() {
        return definition(
            DATASETS_COUNT, "Datasets",
            "Number of published datasets, filterable/groupable by a single access status value (OA, "
                + "Closed, Embargoed, or Restricted), by country, or by classification. Same shape "
                + "and upstream as publications.count, scoped to entity=dataset instead of "
                + "entity=publication.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ), false
                ),
                countryBinding(),
                filterableGroupableBinding(CLASSIFICATION_DIMENSION)
            ),
            TIME_POLICY, OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Software-entity counterpart to publicationsCount()/datasetsCount() — same shape and upstream
     * (OpenAireStatsToolIndicatorQueryHandler, scoped to continent=Europe), just entity=software.
     * Also filterable/groupable by classification (e.g. "Software") — fieldOfScience is
     * publication-only, not evidenced for software.
     */
    private static IndicatorDefinition softwareCount() {
        return definition(
            SOFTWARE_COUNT, "Software",
            "Number of published software, filterable/groupable by a single access status value (OA, "
                + "Closed, Embargoed, or Restricted), by country, or by classification. Same shape "
                + "and upstream as publications.count/datasets.count, scoped to entity=software "
                + "instead of entity=publication/dataset.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ), false
                ),
                countryBinding(),
                filterableGroupableBinding(CLASSIFICATION_DIMENSION)
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
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, IndicatorFormat.PERCENTAGE, RenderHint.SCALAR,
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
     * Open Data page counterpart to publicationsAccessStatusShare() — same shape and upstream
     * (OpenAireStatsToolIndicatorQueryHandler, scoped to continent=Europe), just entity=dataset
     * instead of entity=publication. Satisfies "Open Data in Europe (Open vs Closed)".
     */
    private static IndicatorDefinition datasetsAccessStatusShare() {
        return definition(
            DATASETS_ACCESS_STATUS_SHARE, "Datasets by Access Status (Share)",
            "Percentage share of datasets matching the given access status filter, out of all "
                + "datasets. Same shape as publications.access_status_share, scoped to entity=dataset "
                + "instead of entity=publication. The accessStatus filter is required — there is no "
                + "meaningful value without specifying which status(es) to measure as a share of the "
                + "total.",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, IndicatorFormat.PERCENTAGE, RenderHint.SCALAR,
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
     * Legacy id 69 — "Financial investments in OA Publication", generalized to an optional,
     * groupable investmentArea filter so the same indicator serves every topic area's
     * financial-investment question (survey Question56/68/64/60/100/80/92/72 for
     * OA_PUBLICATION/OPEN_DATA/FAIR_DATA/DATA_MANAGEMENT/CITIZEN_SCIENCE/REPOSITORIES_CONNECT/
     * TRAINING/SOFTWARE — see SurveyStatsToolIndicatorQueryHandler.QUESTION_CODE_BY_INVESTMENT_AREA)
     * rather than being OA-Publication-only. SurveyStatsToolIndicatorQueryHandler applies the
     * ×1,000,000 scale so the returned value is in euros. Filterable/groupable by country (absent
     * country filter aggregates all countries with an answer, per the requested AggregationPolicy —
     * SUM by default). Also filterable/groupable by investmentArea: an absent filter sums every
     * area together first ("Total amount of investments in EOSC and Open Science" — legacy id 63),
     * grouping by it instead fans out into one point per area ("...by Open Science area" — legacy id
     * 64); both compose with country grouping the same way country grouping alone already did.
     * Supports a real multi-year trend: a timeSeries request fetches one point per year; without
     * one, the (optional) surveyYear filter picks a single year.
     */
    private static IndicatorDefinition financialInvestment() {
        return definition(
            FINANCIAL_INVESTMENT, "Financial Investment",
            "Reported financial investment in the given topic area(s), in euros. Survey values are "
                + "collected in millions of euros — this indicator applies an explicit x1,000,000 "
                + "scale before returning a value, so the returned number is already in euros, not "
                + "millions. The investmentArea filter is optional: omitted, every area's investment "
                + "is summed together; grouping by investmentArea instead returns one point per area.",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, IndicatorFormat.CURRENCY, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding(), filterOnlyBinding(SURVEY_YEAR_DIMENSION), filterableGroupableBinding(INVESTMENT_AREA_DIMENSION)),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy ids 73/74 — "National policy"/"National monitoring on OA Publication", collapsed
     * behind a required initiativeType filter (EQ for a single area, IN for several — fixed enum,
     * see InitiativeTypeDimensionMemberProvider). Per-country status, always one row per
     * answering country; grouping by initiativeType additionally fans out one row per (country,
     * initiativeType) pair — this is how the "National Monitoring/Policies of Open Science by
     * Countries and Areas" country x area matrix (legacy ids 68/74) is served: a flat list of
     * triples the caller pivots into a matrix table, no dedicated matrix RenderHint needed. A single
     * survey year, not a real time series — the required surveyYear filter picks it.
     *
     * <p>Three-valued (YES/NO/AWAITING_DATA), not boolean: the underlying survey question is a flat
     * Yes/No radio field with no richer status, so a country that hasn't answered is reported as
     * AWAITING_DATA rather than silently dropped — see
     * SurveyStatsToolIndicatorQueryHandler.fetchInitiativeStatusByCountry. This does not stretch to
     * a 4-state Mandatory/Policy-but-not-mandatory/No-policy/Awaiting-data model for "*_POLICY"
     * members — see countryPolicyStatus() for that.
     *
     * <p>Also filterable/groupable by country, narrowing (or, since every row is already
     * per-country, redundantly re-confirming) the rows returned; absent filter means every
     * answering-or-pending country, as before.
     */
    private static IndicatorDefinition countryInitiativeStatus() {
        return definition(
            COUNTRY_INITIATIVE_STATUS, "Country Initiative Status",
            "Per-country adoption status (YES, NO, or AWAITING_DATA for a country that hasn't "
                + "answered yet) of a national initiative, for the given initiativeType(s) — one "
                + "status per (country, initiativeType) pair when grouped by initiativeType, "
                + "otherwise a single initiativeType is required. No aggregation — always one status "
                + "per answering-or-pending country. Optionally filterable by country to narrow the "
                + "rows returned.",
            IndicatorValueType.TEXT, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, IndicatorFormat.YES_NO, RenderHint.ENTITY_MAP,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(initiativeTypeFilterGroupBinding(), requiredEqFilterBinding(SURVEY_YEAR_DIMENSION), countryBinding()),
            NON_TIME_SERIES_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy ids 68/70/71/72 — "EU Countries with Policy/Immediate-OA-Policy/Financial-Strategy/
     * Monitoring on OA Publication", collapsed behind a required initiativeType filter (EQ for a
     * single area, IN for several). Two views over the same per-country fact as
     * countryInitiativeStatus() — the handler shares one data-access call between them.
     * Aggregates over every EU respondent by default, same as before; an optional country filter
     * narrows both numerator and denominator to just the requested country/countries instead (e.g.
     * "is Greece covered" reduces to 0% or 100%), rather than the always-EU-wide-only behavior this
     * indicator originally had. Also groupable by country: fans out into one 0%/100% row per
     * country (that country's own Yes/no-answer, as a percentage) instead of one blended EU-wide
     * value — a genuinely different, useful shape, unlike the ENTITY_MAP indicators above where
     * country is already the mandatory row dimension regardless of groupBy.
     *
     * <p>Supports a real multi-year trend (a timeSeries request fetches one point per year, like
     * financialInvestment; the surveyYear filter is optional, not required) and fans out one point
     * per initiativeType when grouped by it — combined, this is how "Trends in National Open Science
     * Policies/Financial-Strategy by Areas" (legacy ids 59/60/69/70) is served: one %-per-year series
     * per area, in a single query.
     */
    private static IndicatorDefinition euCountryCoverage() {
        return definition(
            EU_COUNTRY_COVERAGE, "EU Country Coverage",
            "Percentage of respondents with a Yes on the given initiativeType's national initiative "
                + "question, out of all respondents to that question that year (matches "
                + "observatory-ui's own calculation exactly, including any non-EU respondents "
                + "present in the survey). A country with no answer to this specific question still "
                + "counts toward the denominator, just not as a Yes. An optional country filter "
                + "narrows both numerator and denominator to just the requested country/countries "
                + "instead of every EU respondent; grouping by country instead returns one 0%/100% "
                + "row per country. One point per initiativeType when grouped by it; a real "
                + "multi-year trend when a timeSeries range is requested.",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, IndicatorFormat.PERCENTAGE, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(initiativeTypeFilterGroupBinding(), filterOnlyBinding(SURVEY_YEAR_DIMENSION), countryBinding()),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * 4-state (MANDATORY/NON_MANDATORY/NO_POLICY/AWAITING_DATA) counterpart to
     * countryInitiativeStatus(), for the "National Policy on X" rows (legacy ids 7/15/22/28/35/42/
     * 44/50/57/72) that ask for a Mandatory/Policy-but-not-mandatory/No-policy/Awaiting-data
     * breakdown — unlike the original assessment (a now-deleted planning doc found only flat Yes/No
     * policy questions), every "*_POLICY" initiativeType member does carry a nested "Is this policy
     * mandatory?" sub-question, live-verified against both the 2022 and 2023 survey years — see
     * SurveyStatsToolIndicatorQueryHandler.queryCountryPolicyStatus. Only valid for "*_POLICY"
     * members — passing a "*_FINANCIAL_STRATEGY"/"*_MONITORING_INITIATIVE" member has no mandatory
     * sub-question to combine with and is a caller error, not validated here (same as
     * countryInitiativeStatus() trusting its initiativeType filter value).
     *
     * <p>initiativeType is EQ (one policy area) or IN (several), and — like
     * countryInitiativeStatus() — also GROUP-usable: an IN filter plus a groupBy on initiativeType
     * fans out one status per (country, initiativeType) pair, i.e. the "National Open Science
     * Policies by Countries and Areas" country×area matrix.
     *
     * <p>Also filterable/groupable by country, narrowing (or, since every row is already
     * per-country, redundantly re-confirming) the rows returned.
     */
    private static IndicatorDefinition countryPolicyStatus() {
        return definition(
            COUNTRY_POLICY_STATUS, "Country Policy Status",
            "Per-country policy status (MANDATORY, NON_MANDATORY, NO_POLICY, or AWAITING_DATA for a "
                + "country that hasn't answered yet) for the given \"*_POLICY\" initiativeType. "
                + "Combines that policy's base Yes/No question with its nested \"is this policy "
                + "mandatory?\" sub-question — a country with no policy is NO_POLICY outright (the "
                + "mandatory sub-question is skip-logic'd for them in the survey itself, not a "
                + "missing answer). No aggregation — always one status per answering-or-pending "
                + "country. Optionally filterable by country to narrow the rows returned.",
            IndicatorValueType.TEXT, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, IndicatorFormat.CATEGORY, RenderHint.ENTITY_MAP,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(initiativeTypeFilterGroupBinding(), requiredEqFilterBinding(SURVEY_YEAR_DIMENSION), countryBinding()),
            NON_TIME_SERIES_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy id 66 — "National Monitoring (number of monitored areas per country)": for one
     * country, how many of every "*_MONITORING_INITIATIVE" initiativeType member did it answer Yes
     * to, out of all 9 topic areas. No initiativeType filter — the subset is fixed by this
     * indicator's own code, unlike countryInitiativeStatus()/euCountryCoverage(). Always one row
     * per country, same as countryInitiativeStatus() — no aggregation. Also filterable/groupable by
     * country, narrowing (or, since every row is already per-country, redundantly re-confirming)
     * the rows returned.
     */
    private static IndicatorDefinition monitoredAreasCount() {
        return definition(
            MONITORED_AREAS_COUNT, "Monitored Areas Count",
            "Per-country count of topic areas (out of every \"*_MONITORING_INITIATIVE\" "
                + "initiativeType member) with a Yes national-monitoring-initiative answer that "
                + "year. No aggregation — always one count per country. Optionally filterable by "
                + "country to narrow the rows returned.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.ENTITY_MAP,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(requiredEqFilterBinding(SURVEY_YEAR_DIMENSION), countryBinding()),
            NON_TIME_SERIES_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Legacy id 71 — "National Policies (number of policy areas per country)", the policy-side
     * counterpart to monitoredAreasCount(): for one country, how many of every "*_POLICY"
     * initiativeType member did it answer Yes to. Also filterable/groupable by country, narrowing
     * (or, since every row is already per-country, redundantly re-confirming) the rows returned.
     */
    private static IndicatorDefinition policyAreasCount() {
        return definition(
            POLICY_AREAS_COUNT, "Policy Areas Count",
            "Per-country count of topic areas (out of every \"*_POLICY\" initiativeType member) "
                + "with a Yes national-policy answer that year. No aggregation — always one count "
                + "per country. Optionally filterable by country to narrow the rows returned.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.ENTITY_MAP,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(requiredEqFilterBinding(SURVEY_YEAR_DIMENSION), countryBinding()),
            NON_TIME_SERIES_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Inventory row "Coverage of Researchers by National Open Science Policies in Europe" — the
     * share of Europe's researchers who are in a country with a national Open Science policy
     * (conceptually {@code sum(researchers[country] with a policy) / sum(researchers[country])},
     * combining policyAreasCount() with a EUROSTAT per-country researcher count).
     *
     * <p><strong>Registered but not queryable yet.</strong> The researcher-count denominator would
     * come from a EUROSTAT-sourced {@code datasets.DatasetEntry} ingest that this repo has no
     * confirmed migration, seed or fixture for, and no {@code IndicatorQueryHandler} reads
     * {@code datasets} today. It is in the catalog so {@code GET /indicators/presets} covers every
     * inventory row; {@link LocalRegistryIndicatorQueryHandler} answers a query for it with an
     * {@link eu.openaire.observatory.indicator.validation.IndicatorNotYetImplementedException}
     * (HTTP 501) until the data is wired. See {@code docs/INDICATOR_TODO.md}.
     */
    private static IndicatorDefinition researcherPolicyCoverage() {
        return definition(
            RESEARCHER_POLICY_COVERAGE, "Coverage of Researchers by National Open Science Policies",
            "Percentage of Europe's researchers who are in a country with a national Open Science "
                + "policy. Registered for catalog completeness but not queryable yet — its EUROSTAT "
                + "per-country researcher-count denominator is not wired in this deployment; a query "
                + "returns HTTP 501 until it is.",
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, IndicatorFormat.PERCENTAGE, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(requiredEqFilterBinding(SURVEY_YEAR_DIMENSION), countryBinding()),
            NON_TIME_SERIES_POLICY, LocalRegistryIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Survey Question89: "How many repositories offered long-term data preservation in your
     * country?" — plain per-country integer count, structurally identical to financialInvestment()
     * apart from having no unit scale. Question code confirmed live against the real survey
     * stats-tool. Supports a real multi-year trend, same as financialInvestment().
     */
    private static IndicatorDefinition longTermPreservationCount() {
        return definition(
            LONG_TERM_PRESERVATION_COUNT, "Repositories with Long-Term Preservation",
            "Number of repositories offering long-term data preservation, per country (survey "
                + "Question89). Filterable/groupable by country (absent country filter aggregates "
                + "all countries with an answer, per the requested AggregationPolicy — SUM by default).",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding(), filterOnlyBinding(SURVEY_YEAR_DIMENSION)),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Survey Question101: "How many projects with a citizen science dimension were started in
     * your country?" — plain per-country integer count, same shape as longTermPreservationCount().
     * Question code confirmed live against the real survey stats-tool. Supports a real multi-year
     * trend, same as financialInvestment().
     */
    private static IndicatorDefinition citizenScienceNewProjectsCount() {
        return definition(
            CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, "New Citizen Science Projects",
            "Number of new projects with a citizen science dimension started, per country (survey "
                + "Question101). Filterable/groupable by country (absent country filter aggregates "
                + "all countries with an answer, per the requested AggregationPolicy — SUM by default).",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding(), filterOnlyBinding(SURVEY_YEAR_DIMENSION)),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Survey Question73: "How many Open-Source software sets were published in your country?" —
     * plain per-country integer count, same shape as longTermPreservationCount()/
     * citizenScienceNewProjectsCount(). Distinct from software.count, which counts OpenAIRE-indexed
     * software records — this is the survey's self-reported per-country figure instead, a genuinely
     * different data source kept separate rather than collapsed into software.count (see
     * IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT). Question code confirmed live against the real
     * survey stats-tool for both 2022 and 2023. Supports a real multi-year trend, same as
     * financialInvestment().
     */
    private static IndicatorDefinition softwareOpenSourceSetsCount() {
        return definition(
            SOFTWARE_OPEN_SOURCE_SETS_COUNT, "Open-Source Software Sets Published",
            "Number of open-source software sets published, per country, as self-reported in the "
                + "national survey (survey Question73) — distinct from software.count, which counts "
                + "OpenAIRE-indexed software records directly. Filterable/groupable by country "
                + "(absent country filter aggregates all countries with an answer, per the requested "
                + "AggregationPolicy — SUM by default).",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            SUM_AGGREGATION_POLICY,
            null,
            Set.of(countryBinding(), filterOnlyBinding(SURVEY_YEAR_DIMENSION)),
            TIME_POLICY, SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Local-only — no external HTTP call. Backed directly by
     * SurveyServiceImpl.getCountriesWithValidatedAnswer, deduplicated (that method returns one
     * entry per validated answer, not per distinct country). surveyId is a required filter since
     * there's no "latest survey" concept to default to. Also optionally filterable by country,
     * narrowing the count to just the requested country/countries that have a validated answer
     * (absent means every distinct country, as before).
     */
    private static IndicatorDefinition countriesWithValidatedAnswers() {
        return definition(
            COUNTRIES_WITH_VALIDATED_ANSWERS, "Countries with Validated Survey Answers",
            "Number of distinct countries with at least one validated answer for the given survey. "
                + "The surveyId filter is required — there is no meaningful value without "
                + "specifying which survey. An optional country filter narrows the count to just the "
                + "requested country/countries.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(requiredEqFilterBinding(SURVEY_ID_DIMENSION), countryFilterOnlyBinding()),
            NON_TIME_SERIES_POLICY, LocalRegistryIndicatorQueryHandler.HANDLER_KEY
        );
    }

    /**
     * Local-only — no external HTTP call. Backed directly by Stakeholder records of type
     * "eosc-sb" (the EOSC Steering Board country representatives) — hardcoded, not a
     * client-filterable dimension, since this indicator is specifically about EOSC Steering Board
     * country participation, not stakeholder types in general. Optionally filterable by the
     * participating country itself, narrowing the count to just the requested country/countries.
     */
    private static IndicatorDefinition stakeholderParticipatingCountriesCount() {
        return definition(
            STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT, "Participating Countries",
            "Number of distinct countries with an EOSC Steering Board stakeholder registered. An "
                + "optional country filter narrows the count to just the requested country/countries.",
            IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, IndicatorFormat.NUMBER, RenderHint.SCALAR,
            NO_AGGREGATION_POLICY,
            null,
            Set.of(countryFilterOnlyBinding()),
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

    /**
     * EQ/IN-filterable country binding without GROUP usage — for indicators whose result shape
     * doesn't have a client-toggleable per-country breakdown (either because it's a plain scalar, or
     * because it already always groups by country regardless of what the caller asks for). Every
     * indicator in this catalog accepts a country filter (one or more countries OR'd together;
     * absent means unscoped/all-respondents); this is the binding for the ones where GROUP isn't a
     * meaningful additional capability, unlike {@link #countryBinding()}.
     */
    private static IndicatorDimensionBinding countryFilterOnlyBinding() {
        return new IndicatorDimensionBinding(
            COUNTRY_DIMENSION, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ, FilterOperator.IN), false
        );
    }

    /** A single required EQ-only filter dimension — no group usage, no other operators. Used by initiativeType and surveyId. */
    private static IndicatorDimensionBinding requiredEqFilterBinding(String dimensionCode) {
        return new IndicatorDimensionBinding(
            dimensionCode, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), true
        );
    }

    /**
     * Required EQ (single area) or IN (several) filter, also GROUP-usable — used by initiativeType
     * on countryInitiativeStatus()/euCountryCoverage(): EQ picks one area for the original
     * single-area shape, IN + a groupBy fans out one point per area (the country x area matrix /
     * trend-by-area rows).
     */
    private static IndicatorDimensionBinding initiativeTypeFilterGroupBinding() {
        return new IndicatorDimensionBinding(
            INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
            Set.of(FilterOperator.EQ, FilterOperator.IN), true
        );
    }

    /** EQ/IN filterable and groupable, optional — used by classification/fieldOfScience. */
    private static IndicatorDimensionBinding filterableGroupableBinding(String dimensionCode) {
        return new IndicatorDimensionBinding(
            dimensionCode, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP), Set.of(FilterOperator.EQ, FilterOperator.IN), false
        );
    }

    /** A single optional EQ-only filter dimension — no group usage. Used by goldOA/greenOA. */
    private static IndicatorDimensionBinding filterOnlyBinding(String dimensionCode) {
        return new IndicatorDimensionBinding(
            dimensionCode, Set.of(DimensionUsage.FILTER), Set.of(FilterOperator.EQ), false
        );
    }

    /**
     * Fills in the boilerplate every definition shares verbatim: a fresh id, a providerIndicatorKey
     * always equal to the indicator's own code, and the fixed PUBLIC/ACTIVE/version-1 trailer.
     */
    private static IndicatorDefinition definition(
            String code, String label, String description,
            IndicatorValueType valueType, IndicatorSemanticType semanticType, UnitType unit, IndicatorFormat format, RenderHint renderHint,
            AggregationPolicy aggregationPolicy, RatioPolicy ratioPolicy,
            Set<IndicatorDimensionBinding> dimensionBindings, TimePolicy timePolicy, String handlerKey) {
        return new IndicatorDefinition(
            UUID.randomUUID(), code, label, description,
            valueType, semanticType, unit, format, renderHint,
            aggregationPolicy, ratioPolicy, dimensionBindings, timePolicy,
            new IndicatorExecutionBinding(handlerKey, code),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
