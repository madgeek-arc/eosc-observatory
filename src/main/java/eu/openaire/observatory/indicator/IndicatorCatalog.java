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
import eu.openaire.observatory.indicator.openaire.OpenAireBibliometricIndicatorQueryHandler;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static eu.openaire.observatory.indicator.IndicatorCodes.ACCESS_STATUS_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.COUNTRY_INITIATIVE_STATUS;
import static eu.openaire.observatory.indicator.IndicatorCodes.EU_COUNTRY_COVERAGE;
import static eu.openaire.observatory.indicator.IndicatorCodes.FINANCIAL_INVESTMENT;
import static eu.openaire.observatory.indicator.IndicatorCodes.INITIATIVE_TYPE_DIMENSION;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE;
import static eu.openaire.observatory.indicator.IndicatorCodes.PUBLICATIONS_COUNT;

/**
 * In-memory catalog of real indicator definitions. Not a stand-in for a future persistence layer
 * — the 5 definitions it holds today are all real: 2 route to OpenAireBibliometricIndicatorQueryHandler
 * (OpenAIRE bibliometric data), 3 route to SurveyStatsToolIndicatorQueryHandler (survey stats-tool
 * data, scoped to survey years 2022+ — see that handler's Javadoc and docs/INDICATOR_ROADMAP.md
 * for why 2021 is excluded). It previously also held 3 synthetic mock indicators with the same
 * codes as the 3 survey ones; those were removed and later replaced with these real
 * implementations — see docs/INDICATOR_ROADMAP.md for the remaining backlog beyond them.
 */
@Service
public class IndicatorCatalog {

    private static final String GROUP_LABEL = "Publications";

    private static final TimePolicy TIME_POLICY = new TimePolicy(
        true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR,
        TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
    );

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
        this.dimensionsByCode = Map.of(
            COUNTRY_DIMENSION, countryDimension,
            ACCESS_STATUS_DIMENSION, accessStatusDimension,
            INITIATIVE_TYPE_DIMENSION, initiativeTypeDimension
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
            financialInvestment(), countryInitiativeStatus(), euCountryCoverage()
        );
    }

    /**
     * Base publication counts, filterable/groupable by accessStatus (OA vs. Closed/Embargoed/Restricted)
     * and by a single 2-letter country code (filter narrows to one country; group fans out into one row
     * per country). Country absent means all-EU (the handler always scopes to continent=Europe).
     */
    private static IndicatorDefinition publicationsCount() {
        return new IndicatorDefinition(
            UUID.randomUUID(), PUBLICATIONS_COUNT, "Publications",
            "Number of published works, filterable/groupable by a single access status value (OA, "
                + "Closed, Embargoed, or Restricted — never aggregated, unlike "
                + "publications.access_status_share) and by country.",
            GROUP_LABEL, IndicatorValueType.INTEGER, IndicatorSemanticType.MEASURE, UnitType.COUNT, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ), false
                ),
                countryBinding()
            ),
            TIME_POLICY,
            new IndicatorExecutionBinding(OpenAireBibliometricIndicatorQueryHandler.HANDLER_KEY, PUBLICATIONS_COUNT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
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
        return new IndicatorDefinition(
            UUID.randomUUID(), PUBLICATIONS_ACCESS_STATUS_SHARE, "Publications by Access Status (Share)",
            "Percentage share of publications matching the given access status filter, out of all "
                + "publications. A single EQ value returns that exact status's share (e.g. OA, or just "
                + "Embargoed); NOT_EQ or a multi-value IN aggregates several statuses together (e.g. "
                + "NOT_EQ OA for the full non-open share). The accessStatus filter is required — "
                + "there is no meaningful value without specifying which status(es) to measure as a "
                + "share of the total.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION, Set.of(DimensionUsage.FILTER),
                    Set.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.NOT_EQ, FilterOperator.NOT_IN), true
                ),
                countryBinding()
            ),
            TIME_POLICY,
            new IndicatorExecutionBinding(OpenAireBibliometricIndicatorQueryHandler.HANDLER_KEY, PUBLICATIONS_ACCESS_STATUS_SHARE),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /**
     * Legacy id 69 — "Financial investments in OA Publication". Survey Question56, in millions of
     * euros; SurveyStatsToolIndicatorQueryHandler applies the ×1,000,000 scale so the returned
     * value is in euros. Filterable/groupable by country (absent country filter aggregates all
     * countries with an answer, per the requested AggregationPolicy — SUM by default).
     */
    private static IndicatorDefinition financialInvestment() {
        return new IndicatorDefinition(
            UUID.randomUUID(), FINANCIAL_INVESTMENT, "Financial Investment in OA Publication",
            "Reported financial investment in Open Access publication (survey Question56), in euros. "
                + "Survey values are collected in millions of euros — this indicator applies an "
                + "explicit x1,000,000 scale before returning a value, so the returned number is "
                + "already in euros, not millions.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(countryBinding()),
            TIME_POLICY,
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, FINANCIAL_INVESTMENT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
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
        return new IndicatorDefinition(
            UUID.randomUUID(), COUNTRY_INITIATIVE_STATUS, "Country Initiative Status",
            "Per-country adoption status (Yes/No) of a national OA initiative, for a given "
                + "initiativeType (survey Question6/Question6.3/Question7/Question54, one per "
                + "initiativeType value). No aggregation — always one boolean per answering country.",
            GROUP_LABEL, IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER),
                    Set.of(FilterOperator.EQ), true
                )
            ),
            TIME_POLICY,
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, COUNTRY_INITIATIVE_STATUS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
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
        return new IndicatorDefinition(
            UUID.randomUUID(), EU_COUNTRY_COVERAGE, "EU Country Coverage",
            "Percentage of that year's survey respondents with a Yes on the given initiativeType's "
                + "national initiative question, out of all respondents to that question that year "
                + "(matches observatory-ui's own calculation exactly, including any non-EU "
                + "respondents present in the survey). A country with no answer to this specific "
                + "question still counts toward the denominator, just not as a Yes.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(
                new IndicatorDimensionBinding(
                    INITIATIVE_TYPE_DIMENSION, Set.of(DimensionUsage.FILTER),
                    Set.of(FilterOperator.EQ), true
                )
            ),
            TIME_POLICY,
            new IndicatorExecutionBinding(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY, EU_COUNTRY_COVERAGE),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
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
}
