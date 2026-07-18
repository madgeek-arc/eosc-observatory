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

import java.util.Set;
import java.util.UUID;

/** Shared fixtures for indicator-package tests. */
public final class IndicatorFixtures {

    private IndicatorFixtures() {
    }

    public static final String COUNTRY_DIMENSION = "country";

    public static DimensionDefinition countryDimension() {
        return new DimensionDefinition(
            COUNTRY_DIMENSION,
            "Country",
            DimensionType.ENTITY_REFERENCE,
            true,
            true,
            true,
            Set.of(FilterOperator.EQ, FilterOperator.IN),
            null
        );
    }

    /** A MEASURE indicator, time-series enabled, bound to a "local-test-handler" execution binding. */
    public static IndicatorDefinition countIndicator(String code) {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            code,
            "Publications",
            "Number of published works",
            null,
            IndicatorValueType.INTEGER,
            IndicatorSemanticType.MEASURE,
            UnitType.COUNT,
            RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                NullHandling.EXCLUDE
            ),
            null,
            Set.of(new IndicatorDimensionBinding(
                COUNTRY_DIMENSION,
                Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP),
                Set.of(FilterOperator.EQ, FilterOperator.IN),
                false
            )),
            new TimePolicy(
                true,
                "publicationYear",
                Set.of(TimeGrain.YEAR),
                TimeGrain.YEAR,
                TemporalBehavior.PERIOD_VALUE,
                MissingPeriodHandling.ZERO_FILL
            ),
            new IndicatorExecutionBinding(code, code), // handlerKey defaults to the indicator code; override per-test as needed
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /** A RATIO indicator — allowedAggregations deliberately excludes anything the query might ask for. */
    public static IndicatorDefinition ratioIndicator(String code) {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            code,
            "Acceptance rate",
            "Accepted / submitted publications",
            null,
            IndicatorValueType.DECIMAL,
            IndicatorSemanticType.RATIO,
            UnitType.PERCENT,
            RenderHint.SCALAR,
            new AggregationPolicy(
                AggregationType.NONE,
                Set.of(AggregationType.NONE),
                NullHandling.EXCLUDE
            ),
            new RatioPolicy(ZeroDenominatorHandling.NULL),
            Set.of(),
            new TimePolicy(
                false, null, Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.OMIT
            ),
            new IndicatorExecutionBinding(code, code),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /** An indicator with time series unsupported, for TimeSeriesNotSupportedException coverage. */
    public static IndicatorDefinition nonTimeSeriesIndicator(String code) {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            code,
            "Country name",
            "Country display name",
            null,
            IndicatorValueType.TEXT,
            IndicatorSemanticType.ATTRIBUTE,
            UnitType.NONE,
            RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.PRESERVE),
            null,
            Set.of(),
            new TimePolicy(false, null, Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.OMIT),
            new IndicatorExecutionBinding(code, code),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    /** A MEASURE indicator whose sole dimension binding is a required filter (requiredFilter=true). */
    public static IndicatorDefinition requiredFilterIndicator(String code) {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            code,
            "Requires a filter",
            "A measure that cannot be queried without filtering by country",
            null,
            IndicatorValueType.INTEGER,
            IndicatorSemanticType.MEASURE,
            UnitType.COUNT,
            RenderHint.SCALAR,
            new AggregationPolicy(AggregationType.SUM, Set.of(AggregationType.SUM), NullHandling.EXCLUDE),
            null,
            Set.of(new IndicatorDimensionBinding(
                COUNTRY_DIMENSION,
                Set.of(DimensionUsage.FILTER),
                Set.of(FilterOperator.EQ, FilterOperator.IN),
                true
            )),
            new TimePolicy(
                true, "publicationYear", Set.of(TimeGrain.YEAR), TimeGrain.YEAR,
                TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
            ),
            new IndicatorExecutionBinding(code, code),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    public static IndicatorDefinition restrictedIndicator(String code) {
        IndicatorDefinition base = countIndicator(code);
        return new IndicatorDefinition(
            base.id(), base.code(), base.label(), base.description(), base.groupCode(), base.valueType(),
            base.semanticType(), base.unit(), base.renderHint(), base.aggregationPolicy(), base.ratioPolicy(),
            base.dimensions(), base.timePolicy(), base.executionBinding(), IndicatorAccessLevel.RESTRICTED,
            base.status(), base.version()
        );
    }
}
