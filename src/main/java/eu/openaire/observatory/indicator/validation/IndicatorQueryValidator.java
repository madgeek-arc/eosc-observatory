package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.handler.SupportsComparisonQueries;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.List;

/**
 * Compiles a raw IndicatorQuery into a validated IndicatorExecutionPlan, checking it against the
 * indicator's own definition. Handlers only ever see the compiled plan, never the raw query.
 */
@Service
public class IndicatorQueryValidator {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int EARLIEST_SUPPORTED_YEAR = 2015;

    private final IndicatorDefinitionLookup definitions;
    private final DimensionDefinitionLookup dimensions;
    private final IndicatorAuthorizationService authorization;
    private final IndicatorQueryHandlerRegistry handlers;

    public IndicatorQueryValidator(
            IndicatorDefinitionLookup definitions,
            DimensionDefinitionLookup dimensions,
            IndicatorAuthorizationService authorization,
            IndicatorQueryHandlerRegistry handlers) {
        this.definitions = definitions;
        this.dimensions = dimensions;
        this.authorization = authorization;
        this.handlers = handlers;
    }

    public IndicatorExecutionPlan compile(IndicatorQuery query, ActorContext actor) {
        IndicatorDefinition definition = definitions.getRequired(query.indicatorCode());

        if (!actor.hasAccess(definition.accessLevel())) {
            throw new IndicatorAccessDeniedException(definition.code());
        }

        QuerySecurityScope securityScope = authorization.authorize(actor, definition, query);

        AggregationType aggregation = resolveAggregation(definition, query);

        List<ResolvedFilter> filters = query.filters().stream()
            .map(filter -> resolveFilter(definition, filter))
            .toList();

        requireMandatoryFilters(definition, filters);

        List<ResolvedDimension> groupBy = query.groupBy().stream()
            .peek(dimensionCode -> requireDimensionBinding(definition, dimensionCode, DimensionUsage.GROUP))
            .map(ResolvedDimension::new)
            .toList();

        ResolvedTimeRange timeRange = resolveTimeRange(definition, query);
        ComparisonMode comparisonMode = resolveComparisonMode(definition, query, timeRange);

        int limit = query.limit() != null ? query.limit() : DEFAULT_LIMIT;
        List<AggregationType> seriesAggregations = resolveSeriesAggregations(definition, query);

        return new IndicatorExecutionPlan(
            definition, aggregation, filters, groupBy, timeRange, comparisonMode, securityScope, limit,
            seriesAggregations
        );
    }

    private AggregationType resolveAggregation(IndicatorDefinition definition, IndicatorQuery query) {
        AggregationType aggregation = query.aggregation() != null
            ? query.aggregation()
            : definition.aggregationPolicy().defaultAggregation();

        if (definition.semanticType() != IndicatorSemanticType.RATIO
                && !definition.aggregationPolicy().allowedAggregations().contains(aggregation)) {
            throw new InvalidAggregationException(definition.code(), aggregation);
        }

        return aggregation;
    }

    /**
     * Summary aggregation(s) across the whole time series — a different axis from the primary
     * {@code aggregation} (which collapses per-dimension values within a single period). {@code NONE}
     * is always rejected here regardless of semantic type or policy: unlike the primary aggregation,
     * it has no meaning as a summary statistic.
     */
    private List<AggregationType> resolveSeriesAggregations(IndicatorDefinition definition, IndicatorQuery query) {
        for (AggregationType type : query.seriesAggregations()) {
            if (type == AggregationType.NONE) {
                throw new InvalidAggregationException(definition.code(), type);
            }
            if (definition.semanticType() != IndicatorSemanticType.RATIO
                    && !definition.aggregationPolicy().allowedAggregations().contains(type)) {
                throw new InvalidAggregationException(definition.code(), type);
            }
        }
        return query.seriesAggregations();
    }

    private ResolvedFilter resolveFilter(IndicatorDefinition definition, IndicatorFilter filter) {
        IndicatorDimensionBinding binding = requireDimensionBinding(definition, filter.dimension(), DimensionUsage.FILTER);

        var dimensionDefinition = dimensions.getRequired(filter.dimension());
        if (!binding.allowedOperators().contains(filter.operator())
                || !dimensionDefinition.supportedOperators().contains(filter.operator())) {
            throw new UnsupportedOperatorException(filter.dimension(), filter.operator());
        }

        return new ResolvedFilter(filter.dimension(), filter.operator(), filter.values());
    }

    private void requireMandatoryFilters(IndicatorDefinition definition, List<ResolvedFilter> filters) {
        for (IndicatorDimensionBinding binding : definition.dimensionBindings()) {
            if (!binding.requiredFilter()) {
                continue;
            }
            boolean present = filters.stream().anyMatch(filter -> filter.dimensionCode().equals(binding.dimensionCode()));
            if (!present) {
                throw new MissingRequiredFilterException(definition.code(), binding.dimensionCode());
            }
        }
    }

    private ResolvedTimeRange resolveTimeRange(IndicatorDefinition definition, IndicatorQuery query) {
        if (query.timeSeries() == null) {
            return null;
        }

        if (!definition.timePolicy().supported()) {
            throw new TimeSeriesNotSupportedException(definition.code());
        }

        var timeSeries = query.timeSeries();
        Integer from = timeSeries.from();
        Integer to = timeSeries.to();
        int currentYear = Year.now().getValue();
        if (from == null || to == null || from < EARLIEST_SUPPORTED_YEAR || from > to || to > currentYear) {
            throw new InvalidTimeRangeException(from, to, currentYear);
        }

        return new ResolvedTimeRange(
            from,
            to,
            timeSeries.missingPeriods() != null
                ? timeSeries.missingPeriods()
                : definition.timePolicy().missingPeriodHandling()
        );
    }

    private ComparisonMode resolveComparisonMode(IndicatorDefinition definition, IndicatorQuery query, ResolvedTimeRange timeRange) {
        if (query.comparisonMode() == null) {
            return null;
        }

        if (timeRange == null) {
            throw new ComparisonRequiresTimeSeriesException(definition.code());
        }
        if (timeRange.from() == timeRange.to()) {
            throw new InvalidComparisonRangeException(definition.code(), timeRange.from(), timeRange.to());
        }
        if (definition.semanticType() == IndicatorSemanticType.ATTRIBUTE) {
            throw new ComparisonNotSupportedException(definition.code());
        }
        if (!(handlers.getRequired(definition.executionBinding().handlerKey()) instanceof SupportsComparisonQueries)) {
            throw new ComparisonNotSupportedException(definition.code());
        }

        return query.comparisonMode();
    }

    private IndicatorDimensionBinding requireDimensionBinding(
            IndicatorDefinition definition, String dimensionCode, DimensionUsage usage) {
        IndicatorDimensionBinding binding = definition.dimensionBindings().stream()
            .filter(b -> b.dimensionCode().equals(dimensionCode))
            .findFirst()
            .orElseThrow(() -> new UnsupportedDimensionException(definition.code(), dimensionCode));

        if (!binding.usages().contains(usage)) {
            throw new UnsupportedDimensionException(definition.code(), dimensionCode);
        }

        return binding;
    }
}
