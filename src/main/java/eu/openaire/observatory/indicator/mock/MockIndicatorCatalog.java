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
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory catalog for legacy indicators 67-74, standing in until a real persistence/handler
 * layer exists. Each legacy id gets its own IndicatorDefinition (rather than the collapsed,
 * initiativeType-parameterized shapes in LegacyIndicatorCatalogMappingTest), since the collapsing
 * was a future-reuse insight for real handlers, not something this mock's 8-item catalog needs.
 */
@Service
public class MockIndicatorCatalog {

    public static final String COUNTRY_DIMENSION = "country";
    static final String HANDLER_KEY = "mock";

    private static final String GROUP_CODE = "Publications";

    private static final TimePolicy TIME_POLICY = new TimePolicy(
        true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR,
        TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
    );

    private final Map<String, IndicatorDefinition> definitionsByCode;
    private final DimensionDefinition countryDimension;

    public MockIndicatorCatalog() {
        this.countryDimension = new DimensionDefinition(
            COUNTRY_DIMENSION, "Country", DimensionType.ENTITY_REFERENCE,
            true, true, true, Set.of(FilterOperator.EQ, FilterOperator.IN), null
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

    public DimensionDefinition countryDimension() {
        return countryDimension;
    }

    private static List<IndicatorDefinition> allDefinitions() {
        return List.of(
            ratioIndicator("67", "OA Publication in Europe (OA vs Closed)",
                "Share of open access vs. closed publications across Europe."),
            ratioIndicator("68", "EU Countries with Policy on OA Publication",
                "Percentage of EU member states with a national policy on open access publication."),
            financialInvestment(),
            ratioIndicator("70", "EU countries with Policy on Immediate OA Publications",
                "Percentage of EU member states with a national policy on immediate open access publication."),
            ratioIndicator("71", "EU Countries with Financial Strategy on OA Publication",
                "Percentage of EU member states with a national financial strategy on open access publication."),
            ratioIndicator("72", "EU Countries with National OA Publication Monitoring Initiatives",
                "Percentage of EU member states with a national monitoring initiative on open access publication."),
            mapIndicator("73", "National policy on Open Access publications",
                "Per-country status of a national policy on open access publications."),
            mapIndicator("74", "National monitoring on Open Access publications",
                "Per-country status of a national monitoring initiative on open access publications.")
        );
    }

    /** 67, 68, 70, 71, 72 — intrinsically Europe/EU-wide aggregates, no country dimension. */
    private static IndicatorDefinition ratioIndicator(String code, String label, String description) {
        return new IndicatorDefinition(
            UUID.randomUUID(), code, label, description, GROUP_CODE,
            IndicatorValueType.DECIMAL, IndicatorSemanticType.RATIO, UnitType.PERCENT, RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.EXCLUDE),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(),
            TIME_POLICY,
            new IndicatorExecutionBinding(HANDLER_KEY, code),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** 69 — country-filterable/groupable measure. */
    private static IndicatorDefinition financialInvestment() {
        return new IndicatorDefinition(
            UUID.randomUUID(), "69", "Financial investments in OA Publication",
            "Total financial investment in open access publication activities.", GROUP_CODE,
            IndicatorValueType.DECIMAL, IndicatorSemanticType.MEASURE, UnitType.CURRENCY, RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(countryBinding()),
            TIME_POLICY,
            new IndicatorExecutionBinding(HANDLER_KEY, "69"),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }

    /** 73, 74 — per-country boolean status, rendered as a map. */
    private static IndicatorDefinition mapIndicator(String code, String label, String description) {
        return new IndicatorDefinition(
            UUID.randomUUID(), code, label, description, GROUP_CODE,
            IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.PRESERVE),
            null,
            Set.of(countryBinding()),
            TIME_POLICY,
            new IndicatorExecutionBinding(HANDLER_KEY, code),
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
