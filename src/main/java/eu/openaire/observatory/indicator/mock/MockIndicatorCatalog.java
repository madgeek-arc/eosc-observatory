package eu.openaire.observatory.indicator.mock;

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
 * In-memory catalog standing in until a real persistence/handler layer exists. Mirrors the
 * collapsed model proven in LegacyIndicatorCatalogMappingTest (5 definitions instead of 8
 * per-legacy-id ones) so a dynamic UI builder can browse real definitions, discover their real
 * dimension capabilities (including the initiativeType filter), and query them directly — no
 * legacy-id translation layer needed. One deliberate deviation from that test: every definition
 * here is time-enabled (year grain, zero-fill), since the builder needs single/multi-year support
 * uniformly, including for the two definitions the test modeled as SNAPSHOT-only.
 */
@Service
public class MockIndicatorCatalog {

    private static final String GROUP_LABEL = "Publications";

    private static final TimePolicy TIME_POLICY = new TimePolicy(
        true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR,
        TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
    );

    private final Map<String, IndicatorDefinition> definitionsByCode;
    private final Map<String, DimensionDefinition> dimensionsByCode;

    public MockIndicatorCatalog() {
        DimensionDefinition countryDimension = new DimensionDefinition(
            COUNTRY_DIMENSION, "Country", DimensionType.ENTITY_REFERENCE,
            true, true, true, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition initiativeTypeDimension = new DimensionDefinition(
            INITIATIVE_TYPE_DIMENSION, "OA initiative type", DimensionType.ENUM,
            true, false, false, Set.of(FilterOperator.EQ, FilterOperator.IN), null
        );
        DimensionDefinition accessStatusDimension = new DimensionDefinition(
            ACCESS_STATUS_DIMENSION, "Access status", DimensionType.ENUM,
            true, true, false,
            Set.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.NOT_EQ, FilterOperator.NOT_IN), null
        );
        this.dimensionsByCode = Map.of(
            COUNTRY_DIMENSION, countryDimension,
            INITIATIVE_TYPE_DIMENSION, initiativeTypeDimension,
            ACCESS_STATUS_DIMENSION, accessStatusDimension
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
            publicationsCount(), publicationsAccessStatusShare(), financialInvestment(),
            countryInitiativeStatus(), euCountryCoverage()
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

    /** Legacy id 69 — "Financial investments in OA Publication", country filterable/groupable. */
    private static IndicatorDefinition financialInvestment() {
        return new IndicatorDefinition(
            UUID.randomUUID(), FINANCIAL_INVESTMENT, "Financial Investment in OA Publication",
            "Total financial investment in open access publication activities.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(countryBinding()),
            TIME_POLICY,
            new IndicatorExecutionBinding(MockIndicatorQueryHandler.HANDLER_KEY, FINANCIAL_INVESTMENT),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** Legacy ids 73 & 74 — per-country boolean status, rendered as a map, requires initiativeType. */
    private static IndicatorDefinition countryInitiativeStatus() {
        return new IndicatorDefinition(
            UUID.randomUUID(), COUNTRY_INITIATIVE_STATUS, "National OA Initiative Status",
            "Per-country status (adopted / not adopted) of a national OA initiative — policy, "
                + "immediate-OA policy, financial strategy, or monitoring, selected via initiativeType.",
            GROUP_LABEL, IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.PRESERVE),
            null,
            Set.of(countryBinding(), requiredInitiativeTypeBinding()),
            TIME_POLICY,
            new IndicatorExecutionBinding(MockIndicatorQueryHandler.HANDLER_KEY, COUNTRY_INITIATIVE_STATUS),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** Legacy ids 68, 70, 71 & 72 — % of EU countries with a given initiative adopted. */
    private static IndicatorDefinition euCountryCoverage() {
        return new IndicatorDefinition(
            UUID.randomUUID(), EU_COUNTRY_COVERAGE, "EU Country Coverage of OA Initiative",
            "Percentage of EU member states with a given national OA initiative adopted, selected via initiativeType.",
            GROUP_LABEL, IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(requiredInitiativeTypeBinding()),
            TIME_POLICY,
            new IndicatorExecutionBinding(MockIndicatorQueryHandler.HANDLER_KEY, EU_COUNTRY_COVERAGE),
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

    private static IndicatorDimensionBinding requiredInitiativeTypeBinding() {
        return new IndicatorDimensionBinding(
            INITIATIVE_TYPE_DIMENSION,
            Set.of(DimensionUsage.FILTER),
            Set.of(FilterOperator.EQ),
            true
        );
    }
}
