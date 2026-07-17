package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.model.DimensionType;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionRef;
import eu.openaire.observatory.indicator.model.IndicatorKind;
import eu.openaire.observatory.indicator.model.IndicatorStatus;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.NullHandling;
import eu.openaire.observatory.indicator.model.TemporalBehavior;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;

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

    /** A COUNT indicator, time-series enabled, sourced by whichever handler the test wires up. */
    public static IndicatorDefinition countIndicator(String code) {
        return new IndicatorDefinition(
            UUID.randomUUID(),
            code,
            "Publications",
            "Number of published works",
            IndicatorValueType.INTEGER,
            IndicatorKind.COUNT,
            UnitType.COUNT,
            new AggregationPolicy(
                AggregationType.SUM,
                Set.of(AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX),
                null,
                null,
                null,
                NullHandling.EXCLUDE
            ),
            Set.of(new IndicatorDimensionRef(COUNTRY_DIMENSION, false)),
            new TimePolicy(
                true,
                "publicationYear",
                Set.of(TimeGrain.YEAR),
                TimeGrain.YEAR,
                TemporalBehavior.PERIOD_VALUE,
                MissingPeriodHandling.ZERO_FILL
            ),
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
            IndicatorValueType.DECIMAL,
            IndicatorKind.RATIO,
            UnitType.PERCENT,
            new AggregationPolicy(
                AggregationType.NONE,
                Set.of(AggregationType.NONE),
                "publications.accepted",
                "publications.submitted",
                AggregationType.SUM,
                NullHandling.EXCLUDE
            ),
            Set.of(),
            new TimePolicy(
                false, null, Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.OMIT
            ),
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
            IndicatorValueType.TEXT,
            IndicatorKind.ATTRIBUTE,
            UnitType.NONE,
            new AggregationPolicy(
                AggregationType.NONE, Set.of(AggregationType.NONE), null, null, null, NullHandling.PRESERVE
            ),
            Set.of(),
            new TimePolicy(false, null, Set.of(), null, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.OMIT),
            IndicatorAccessLevel.PUBLIC,
            IndicatorStatus.ACTIVE,
            1
        );
    }

    public static IndicatorDefinition restrictedIndicator(String code) {
        IndicatorDefinition base = countIndicator(code);
        return new IndicatorDefinition(
            base.id(), base.code(), base.label(), base.description(), base.valueType(), base.kind(),
            base.unit(), base.aggregationPolicy(), base.dimensions(), base.timePolicy(),
            IndicatorAccessLevel.RESTRICTED, base.status(), base.version()
        );
    }
}
