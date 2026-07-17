package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionRef;
import eu.openaire.observatory.indicator.model.IndicatorKind;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;

import java.util.List;

/**
 * Compiles a raw IndicatorQuery into a validated IndicatorExecutionPlan, checking it against the
 * indicator's own definition. Handlers only ever see the compiled plan, never the raw query.
 */
public class IndicatorQueryValidator {

    private static final int DEFAULT_LIMIT = 1000;

    private final IndicatorDefinitionLookup definitions;
    private final DimensionDefinitionLookup dimensions;

    public IndicatorQueryValidator(IndicatorDefinitionLookup definitions, DimensionDefinitionLookup dimensions) {
        this.definitions = definitions;
        this.dimensions = dimensions;
    }

    public IndicatorExecutionPlan compile(IndicatorQuery query, ActorContext actor) {
        IndicatorDefinition definition = definitions.getRequired(query.indicatorCode());

        if (!actor.hasAccess(definition.accessLevel())) {
            throw new IndicatorAccessDeniedException(definition.code());
        }

        AggregationType aggregation = resolveAggregation(definition, query);

        List<ResolvedFilter> filters = query.filters().stream()
            .map(filter -> resolveFilter(definition, filter))
            .toList();

        List<ResolvedDimension> groupBy = query.groupBy().stream()
            .peek(dimensionCode -> requireDimensionRef(definition, dimensionCode))
            .map(ResolvedDimension::new)
            .toList();

        ResolvedTimeRange timeRange = resolveTimeRange(definition, query);

        int limit = query.limit() != null ? query.limit() : DEFAULT_LIMIT;

        return new IndicatorExecutionPlan(definition, aggregation, filters, groupBy, timeRange, limit);
    }

    private AggregationType resolveAggregation(IndicatorDefinition definition, IndicatorQuery query) {
        AggregationType aggregation = query.aggregation() != null
            ? query.aggregation()
            : definition.aggregationPolicy().defaultAggregation();

        if (definition.kind() != IndicatorKind.RATIO
                && !definition.aggregationPolicy().allowedAggregations().contains(aggregation)) {
            throw new InvalidAggregationException(definition.code(), aggregation);
        }

        return aggregation;
    }

    private ResolvedFilter resolveFilter(IndicatorDefinition definition, IndicatorFilter filter) {
        requireDimensionRef(definition, filter.dimension());

        var dimensionDefinition = dimensions.getRequired(filter.dimension());
        if (!dimensionDefinition.supportedOperators().contains(filter.operator())) {
            throw new UnsupportedOperatorException(filter.dimension(), filter.operator());
        }

        return new ResolvedFilter(filter.dimension(), filter.operator(), filter.values());
    }

    private ResolvedTimeRange resolveTimeRange(IndicatorDefinition definition, IndicatorQuery query) {
        if (query.timeSeries() == null) {
            return null;
        }

        if (!definition.timePolicy().supported()) {
            throw new TimeSeriesNotSupportedException(definition.code());
        }

        var timeSeries = query.timeSeries();
        return new ResolvedTimeRange(
            timeSeries.grain(),
            timeSeries.from(),
            timeSeries.to(),
            timeSeries.missingPeriods() != null
                ? timeSeries.missingPeriods()
                : definition.timePolicy().missingPeriodHandling()
        );
    }

    private IndicatorDimensionRef requireDimensionRef(IndicatorDefinition definition, String dimensionCode) {
        return definition.dimensions().stream()
            .filter(ref -> ref.dimensionCode().equals(dimensionCode))
            .findFirst()
            .orElseThrow(() -> new UnsupportedDimensionException(definition.code(), dimensionCode));
    }
}
