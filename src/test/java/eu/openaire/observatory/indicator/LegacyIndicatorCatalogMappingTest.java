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
import eu.openaire.observatory.indicator.model.TemporalBehavior;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.model.ZeroDenominatorHandling;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.validation.ActorContext;
import eu.openaire.observatory.indicator.validation.AllowAllIndicatorAuthorizationService;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;
import eu.openaire.observatory.indicator.validation.IndicatorDefinitionLookup;
import eu.openaire.observatory.indicator.validation.IndicatorQueryValidator;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import eu.openaire.observatory.indicator.validation.UnsupportedDimensionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Maps 8 real legacy indicator definitions (ids 67-74, supplied in a different {@code
 * {id, label, format, group}} shape) onto the current semantic model. The point of this class:
 * several of the 8 are just differently-filtered views of the same underlying concept, so they
 * resolve to 5 distinct {@link IndicatorDefinition}s rather than 8 — e.g. "EU Countries with
 * Policy on OA Publication" (68), "...Policy on Immediate OA Publications" (70), "...Financial
 * Strategy..." (71) and "...Monitoring Initiatives" (72) are the same coverage-ratio shape over
 * different {@code initiativeType} values, not four separate indicators.
 *
 * <p>Geographic scope ("in Europe" for 67, "EU countries" for 68/70/71/72) is intentionally not
 * exposed as a client-filterable dimension — the current model has no intrinsic/fixed-filter
 * concept, so exposing {@code country} as a
 * FILTER there would incorrectly imply a client can override or widen the scope. See
 * {@link #legacyIndicator67_oaShareInEurope_countryIsNotClientFilterable()}.
 */
class LegacyIndicatorCatalogMappingTest {

    private static final String PUBLICATIONS_COUNT = "publications.count";
    private static final String PUBLICATIONS_OA_SHARE = "publications.oa_share";
    private static final String FINANCIAL_INVESTMENT = "oa.financial_investment";
    private static final String COUNTRY_INITIATIVE_STATUS = "oa.country_initiative_status";
    private static final String EU_COUNTRY_COVERAGE = "oa.eu_country_coverage";

    private static final String COUNTRY_DIMENSION = IndicatorFixtures.COUNTRY_DIMENSION;
    private static final String ACCESS_STATUS_DIMENSION = "accessStatus";
    private static final String INITIATIVE_TYPE_DIMENSION = "initiativeType";

    // --- Dimensions -------------------------------------------------------------------------

    private static DimensionDefinition accessStatusDimension() {
        return new DimensionDefinition(
            ACCESS_STATUS_DIMENSION,
            "Access status",
            DimensionType.ENUM,
            true,
            true,
            false,
            Set.of(FilterOperator.EQ, FilterOperator.IN),
            null
        );
    }

    private static DimensionDefinition initiativeTypeDimension() {
        return new DimensionDefinition(
            INITIATIVE_TYPE_DIMENSION,
            "OA initiative type",
            DimensionType.ENUM,
            true,
            true,
            false,
            Set.of(FilterOperator.EQ, FilterOperator.IN),
            null
        );
    }

    // --- Definitions --------------------------------------------------------------------------

    /**
     * Base publication counts. Not one of the 8 legacy ids itself, but the concrete illustration
     * of "OA publications are a filtered subset of Publications" — legacy id 67 is derived from
     * this measure split by {@code accessStatus}, not a standalone dataset.
     */
    private static IndicatorDefinition publicationsCount() {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            PUBLICATIONS_COUNT,
            "Publications",
            "Number of published works, filterable by access status (OA vs. Closed) and country.",
            IndicatorValueType.INTEGER,
            IndicatorSemanticType.MEASURE,
            UnitType.COUNT,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    COUNTRY_DIMENSION,
                    Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN),
                    false
                ),
                new IndicatorDimensionBinding(
                    ACCESS_STATUS_DIMENSION,
                    Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN),
                    false
                )
            ),
            new TimePolicy(
                true, "publicationYear", Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
            ),
            new IndicatorExecutionBinding(PUBLICATIONS_COUNT, PUBLICATIONS_COUNT),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /** Legacy id 67 — "OA Publication in Europe (OA vs Closed)", format percentage. */
    private static IndicatorDefinition publicationsOaShare() {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            PUBLICATIONS_OA_SHARE,
            "OA Publication Share (Europe)",
            "Share of open access vs. closed publications. Scope is intrinsically limited to "
                + "Europe and is not selectable via query filters — the handler applies it "
                + "internally.",
            IndicatorValueType.DECIMAL,
            IndicatorSemanticType.RATIO,
            UnitType.PERCENT,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(),
            new TimePolicy(
                true, "publicationYear", Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.OMIT
            ),
            new IndicatorExecutionBinding(PUBLICATIONS_OA_SHARE, PUBLICATIONS_OA_SHARE),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /** Legacy id 69 — "Financial investments in OA Publication", format number. */
    private static IndicatorDefinition financialInvestment() {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            FINANCIAL_INVESTMENT,
            "Financial Investment in OA Publication",
            "Total financial investment in open access publication activities.",
            IndicatorValueType.DECIMAL,
            IndicatorSemanticType.MEASURE,
            UnitType.CURRENCY,
            new AggregationPolicy(AggregationType.SUM, Set.of(AggregationType.SUM), NullHandling.EXCLUDE),
            null,
            Set.of(new IndicatorDimensionBinding(
                COUNTRY_DIMENSION,
                Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                Set.of(FilterOperator.EQ, FilterOperator.IN),
                false
            )),
            new TimePolicy(
                true, "year", Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
            ),
            new IndicatorExecutionBinding(FINANCIAL_INVESTMENT, FINANCIAL_INVESTMENT),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /**
     * Legacy ids 73 & 74 ("National policy on Open Access publications" / "National monitoring on
     * Open Access publications", both format {@code map}) — collapsed into one ATTRIBUTE
     * definition parameterized by {@code initiativeType}, since both are the same per-country
     * boolean-fact shape rendered as a choropleth.
     */
    private static IndicatorDefinition countryInitiativeStatus() {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            COUNTRY_INITIATIVE_STATUS,
            "National OA Initiative Status",
            "Per-country status (adopted / not adopted) of a national OA initiative — policy, "
                + "immediate-OA policy, financial strategy, or monitoring, selected via "
                + "initiativeType. Rendered as a per-country map; requires grouping by country "
                + "(not yet enforced by the model).",
            IndicatorValueType.BOOLEAN,
            IndicatorSemanticType.ATTRIBUTE,
            UnitType.NONE,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.PRESERVE),
            null,
            Set.of(
                new IndicatorDimensionBinding(
                    COUNTRY_DIMENSION,
                    Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                    Set.of(FilterOperator.EQ, FilterOperator.IN),
                    false
                ),
                new IndicatorDimensionBinding(
                    INITIATIVE_TYPE_DIMENSION,
                    Set.of(DimensionUsage.FILTER),
                    Set.of(FilterOperator.EQ),
                    true
                )
            ),
            new TimePolicy(false, null, Set.of(), null, TemporalBehavior.SNAPSHOT, MissingPeriodHandling.OMIT),
            new IndicatorExecutionBinding(COUNTRY_INITIATIVE_STATUS, COUNTRY_INITIATIVE_STATUS),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /**
     * Legacy ids 68, 70, 71 & 72 ("EU Countries with Policy on OA Publication" / "...Policy on
     * Immediate OA Publications" / "...Financial Strategy on OA Publication" / "...Monitoring
     * Initiatives") — collapsed into one RATIO coverage definition parameterized by
     * {@code initiativeType}, since all four are "% of EU countries where some boolean fact is
     * true," differing only in which fact.
     */
    private static IndicatorDefinition euCountryCoverage() {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            EU_COUNTRY_COVERAGE,
            "EU Country Coverage of OA Initiative",
            "Percentage of EU member states with a given national OA initiative adopted, "
                + "selected via initiativeType. Scope is intrinsically limited to EU member "
                + "states and is not selectable via query filters — the handler applies it "
                + "internally.",
            IndicatorValueType.DECIMAL,
            IndicatorSemanticType.RATIO,
            UnitType.PERCENT,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(new IndicatorDimensionBinding(
                INITIATIVE_TYPE_DIMENSION,
                Set.of(DimensionUsage.FILTER),
                Set.of(FilterOperator.EQ),
                true
            )),
            new TimePolicy(false, null, Set.of(), null, TemporalBehavior.SNAPSHOT, MissingPeriodHandling.OMIT),
            new IndicatorExecutionBinding(EU_COUNTRY_COVERAGE, EU_COUNTRY_COVERAGE),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    // --- Wiring -------------------------------------------------------------------------------

    private final Map<String, IndicatorDefinition> definitionsByCode = List.of(
        publicationsCount(), publicationsOaShare(), financialInvestment(),
        countryInitiativeStatus(), euCountryCoverage()
    ).stream().collect(Collectors.toMap(IndicatorDefinition::code, d -> d));

    private final Map<String, DimensionDefinition> dimensionsByCode = Map.of(
        COUNTRY_DIMENSION, IndicatorFixtures.countryDimension(),
        ACCESS_STATUS_DIMENSION, accessStatusDimension(),
        INITIATIVE_TYPE_DIMENSION, initiativeTypeDimension()
    );

    private final IndicatorDefinitionLookup definitionLookup = code -> {
        var definition = definitionsByCode.get(code);
        if (definition == null) {
            throw new UnknownIndicatorException(code);
        }
        return definition;
    };

    private final DimensionDefinitionLookup dimensionLookup = code -> {
        var dimension = dimensionsByCode.get(code);
        if (dimension == null) {
            throw new IllegalStateException("unexpected dimension in test: " + code);
        }
        return dimension;
    };

    private final IndicatorQueryValidator validator = new IndicatorQueryValidator(
        definitionLookup, dimensionLookup, new AllowAllIndicatorAuthorizationService()
    );

    private static final ActorContext ALLOW_ALL = level -> true;

    private static IndicatorQuery baseQuery(String code) {
        return new IndicatorQuery(code, null, List.of(), List.of(), null, List.of(), null);
    }

    private static IndicatorQuery filteredQuery(String code, IndicatorFilter filter, List<String> groupBy) {
        return new IndicatorQuery(code, null, List.of(filter), groupBy, null, List.of(), null);
    }

    // --- The user's own example: OA publications are a filtered subset of Publications --------

    @Test
    void oaPublicationsAreAFilteredSubsetOfPublications() {
        var oaOnly = filteredQuery(
            PUBLICATIONS_COUNT,
            new IndicatorFilter(ACCESS_STATUS_DIMENSION, FilterOperator.IN, List.of("OA")),
            List.of()
        );
        var allPublications = baseQuery(PUBLICATIONS_COUNT);

        var oaPlan = validator.compile(oaOnly, ALLOW_ALL);
        var allPlan = validator.compile(allPublications, ALLOW_ALL);

        assertThat(oaPlan.definition().code()).isEqualTo(allPlan.definition().code());
        assertThat(oaPlan.filters()).hasSize(1);
        assertThat(oaPlan.filters().get(0).dimensionCode()).isEqualTo(ACCESS_STATUS_DIMENSION);
        assertThat(allPlan.filters()).isEmpty();
    }

    // --- Legacy id 67 --------------------------------------------------------------------------

    @Test
    void legacyIndicator67_oaShareInEurope() {
        var plan = validator.compile(baseQuery(PUBLICATIONS_OA_SHARE), ALLOW_ALL);

        assertThat(plan.definition().semanticType()).isEqualTo(IndicatorSemanticType.RATIO);
        assertThat(plan.definition().unit()).isEqualTo(UnitType.PERCENT);
    }

    @Test
    void legacyIndicator67_oaShareInEurope_countryIsNotClientFilterable() {
        var query = filteredQuery(
            PUBLICATIONS_OA_SHARE,
            new IndicatorFilter(COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")),
            List.of()
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    // --- Legacy ids 68, 70, 71, 72 — same definition, different initiativeType -----------------

    @Test
    void legacyIndicator68_euCountriesWithOaPublicationPolicy() {
        var query = filteredQuery(
            EU_COUNTRY_COVERAGE,
            new IndicatorFilter(INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_POLICY")),
            List.of()
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().semanticType()).isEqualTo(IndicatorSemanticType.RATIO);
        assertThat(plan.filters()).hasSize(1);
        assertThat(plan.filters().get(0).values()).containsExactly("OA_PUBLICATION_POLICY");
    }

    @Test
    void legacyIndicator70_euCountriesWithImmediateOaPolicy() {
        var query = filteredQuery(
            EU_COUNTRY_COVERAGE,
            new IndicatorFilter(INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("IMMEDIATE_OA_POLICY")),
            List.of()
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(EU_COUNTRY_COVERAGE);
        assertThat(plan.filters().get(0).values()).containsExactly("IMMEDIATE_OA_POLICY");
    }

    @Test
    void legacyIndicator71_euCountriesWithFinancialStrategy() {
        var query = filteredQuery(
            EU_COUNTRY_COVERAGE,
            new IndicatorFilter(INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("FINANCIAL_STRATEGY")),
            List.of()
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(EU_COUNTRY_COVERAGE);
        assertThat(plan.filters().get(0).values()).containsExactly("FINANCIAL_STRATEGY");
    }

    @Test
    void legacyIndicator72_euCountriesWithMonitoringInitiatives() {
        var query = filteredQuery(
            EU_COUNTRY_COVERAGE,
            new IndicatorFilter(INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("MONITORING_INITIATIVE")),
            List.of()
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(EU_COUNTRY_COVERAGE);
        assertThat(plan.filters().get(0).values()).containsExactly("MONITORING_INITIATIVE");
    }

    @Test
    void euCountryCoverage_requiresInitiativeTypeFilter_countryIsNotClientFilterable() {
        var query = filteredQuery(
            EU_COUNTRY_COVERAGE,
            new IndicatorFilter(COUNTRY_DIMENSION, FilterOperator.EQ, List.of("GR")),
            List.of()
        );

        assertThatThrownBy(() -> validator.compile(query, ALLOW_ALL))
            .isInstanceOf(UnsupportedDimensionException.class);
    }

    // --- Legacy id 69 --------------------------------------------------------------------------

    @Test
    void legacyIndicator69_financialInvestmentInOaPublication() {
        var plan = validator.compile(baseQuery(FINANCIAL_INVESTMENT), ALLOW_ALL);

        assertThat(plan.definition().semanticType()).isEqualTo(IndicatorSemanticType.MEASURE);
        assertThat(plan.definition().unit()).isEqualTo(UnitType.CURRENCY);
    }

    // --- Legacy ids 73, 74 — same definition, different initiativeType, grouped by country ------

    @Test
    void legacyIndicator73_nationalPolicyOnOaPublications_map() {
        var query = filteredQuery(
            COUNTRY_INITIATIVE_STATUS,
            new IndicatorFilter(INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("OA_PUBLICATION_POLICY")),
            List.of(COUNTRY_DIMENSION)
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().semanticType()).isEqualTo(IndicatorSemanticType.ATTRIBUTE);
        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(COUNTRY_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("OA_PUBLICATION_POLICY");
    }

    @Test
    void legacyIndicator74_nationalMonitoringOnOaPublications_map() {
        var query = filteredQuery(
            COUNTRY_INITIATIVE_STATUS,
            new IndicatorFilter(INITIATIVE_TYPE_DIMENSION, FilterOperator.EQ, List.of("MONITORING_INITIATIVE")),
            List.of(COUNTRY_DIMENSION)
        );

        var plan = validator.compile(query, ALLOW_ALL);

        assertThat(plan.definition().code()).isEqualTo(COUNTRY_INITIATIVE_STATUS);
        assertThat(plan.groupBy()).extracting("dimensionCode").containsExactly(COUNTRY_DIMENSION);
        assertThat(plan.filters().get(0).values()).containsExactly("MONITORING_INITIATIVE");
    }
}
