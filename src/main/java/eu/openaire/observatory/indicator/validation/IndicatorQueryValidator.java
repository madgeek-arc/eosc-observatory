package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import eu.openaire.observatory.indicator.handler.IndicatorQueryHandlerRegistry;
import eu.openaire.observatory.indicator.handler.SupportsComparisonQueries;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorDimensionBinding;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.query.ComparisonMode;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Compiles a raw IndicatorQuery into a validated IndicatorExecutionPlan, checking it against the
 * indicator's own definition. Handlers only ever see the compiled plan, never the raw query.
 */
@Service
public class IndicatorQueryValidator {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int EARLIEST_SUPPORTED_YEAR = 2015;
    private static final Set<FilterOperator> VALUE_CHECKABLE_OPERATORS =
        Set.of(FilterOperator.EQ, FilterOperator.NOT_EQ, FilterOperator.IN, FilterOperator.NOT_IN);

    private final IndicatorDefinitionLookup definitions;
    private final DimensionDefinitionLookup dimensions;
    private final IndicatorAuthorizationService authorization;
    private final IndicatorQueryHandlerRegistry handlers;
    private final DimensionMemberProviderRegistry memberProviders;

    public IndicatorQueryValidator(
            IndicatorDefinitionLookup definitions,
            DimensionDefinitionLookup dimensions,
            IndicatorAuthorizationService authorization,
            IndicatorQueryHandlerRegistry handlers,
            DimensionMemberProviderRegistry memberProviders) {
        this.definitions = definitions;
        this.dimensions = dimensions;
        this.authorization = authorization;
        this.handlers = handlers;
        this.memberProviders = memberProviders;
    }

    public IndicatorExecutionPlan compile(IndicatorQuery query, ActorContext actor) {
        IndicatorDefinition definition = definitions.getRequired(query.indicatorCode());

        if (!actor.hasAccess(definition.accessLevel())) {
            throw new IndicatorAccessDeniedException(definition.code());
        }

        QuerySecurityScope securityScope = authorization.authorize(actor, definition, query);

        AggregationType aggregation = resolveAggregation(definition, query);

        validateFilterShapes(query.filters());
        List<ResolvedFilter> filters = query.filters().stream()
            .map(filter -> resolveFilter(definition, filter))
            .toList();

        requireMandatoryFilters(definition, filters);

        List<ResolvedDimension> groupBy = query.groupBy().stream()
            .peek(dimensionCode -> requireDimensionBinding(definition, dimensionCode, DimensionUsage.GROUP))
            .map(ResolvedDimension::new)
            .toList();

        requireGroupByForMultiValuedFilters(definition, filters, query.groupBy());

        ResolvedTimeRange timeRange = resolveTimeRange(definition, query);
        requireTimeScope(definition, query, timeRange);
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

    /** Check the entire request before any member lookup, which may access a remote provider. */
    private void validateFilterShapes(List<IndicatorFilter> filters) {
        Set<String> seenDimensions = new HashSet<>();
        for (int index = 0; index < filters.size(); index++) {
            IndicatorFilter filter = filters.get(index);
            String label = "at index " + index;
            if (filter == null) {
                throw new InvalidFilterShapeException(label, "must not be null");
            }
            if (filter.dimension() == null || filter.dimension().isBlank()) {
                throw new InvalidFilterShapeException(label, "dimension must not be blank");
            }
            label = filter.dimension();
            if (!seenDimensions.add(label)) {
                throw new InvalidFilterShapeException(label, "dimension must occur only once");
            }
            if (filter.operator() == null) {
                throw new InvalidFilterShapeException(label, "operator must not be null");
            }
            if (filter.values() == null || filter.values().stream().anyMatch(Objects::isNull)) {
                throw new InvalidFilterShapeException(label, "values must be a non-null list without null elements");
            }
            int size = filter.values().size();
            boolean valid = switch (filter.operator()) {
                case EQ, NOT_EQ, GT, GTE, LT, LTE, CONTAINS, STARTS_WITH -> size == 1;
                case BETWEEN -> size == 2;
                case IN, NOT_IN -> size > 0;
                case IS_NULL, IS_NOT_NULL -> size == 0;
            };
            if (!valid) {
                String expected = switch (filter.operator()) {
                    case EQ, NOT_EQ, GT, GTE, LT, LTE, CONTAINS, STARTS_WITH -> "exactly one value";
                    case BETWEEN -> "exactly two values";
                    case IN, NOT_IN -> "at least one value";
                    case IS_NULL, IS_NOT_NULL -> "no values";
                };
                throw new InvalidFilterShapeException(label, filter.operator() + " requires " + expected);
            }
        }
    }

    private void requireGroupByForMultiValuedFilters(
            IndicatorDefinition definition, List<ResolvedFilter> filters, List<String> groupBy) {
        for (ResolvedFilter filter : filters) {
            if (filter.operator() != FilterOperator.IN || filter.values().size() < 2 || groupBy.contains(filter.dimensionCode())) {
                continue;
            }
            boolean required = definition.dimensionBindings().stream()
                .anyMatch(binding -> binding.dimensionCode().equals(filter.dimensionCode())
                    && binding.groupByRequiredForMultipleValues());
            if (required) {
                throw new InvalidFilterShapeException(filter.dimensionCode(),
                    "several values require grouping by " + filter.dimensionCode());
            }
        }
    }

    private ResolvedFilter resolveFilter(IndicatorDefinition definition, IndicatorFilter filter) {
        IndicatorDimensionBinding binding = requireDimensionBinding(definition, filter.dimension(), DimensionUsage.FILTER);

        var dimensionDefinition = dimensions.getRequired(filter.dimension());
        if (!binding.allowedOperators().contains(filter.operator())
                || !dimensionDefinition.supportedOperators().contains(filter.operator())) {
            throw new UnsupportedOperatorException(filter.dimension(), filter.operator());
        }

        List<Object> values = IndicatorCodes.SURVEY_YEAR_DIMENSION.equals(filter.dimension())
            ? normalizeSurveyYearValues(filter) : List.copyOf(filter.values());
        if (filter.operator() == FilterOperator.IN || filter.operator() == FilterOperator.NOT_IN) {
            values = values.stream().distinct().toList();
        }
        validateFilterValues(definition.code(), new IndicatorFilter(filter.dimension(), filter.operator(), values));

        return new ResolvedFilter(filter.dimension(), filter.operator(), values);
    }

    /**
     * Rejects filter values that aren't a real member of their dimension, for any dimension with a
     * registered {@code DimensionMemberProvider} — nothing to validate against otherwise.
     */
    private void validateFilterValues(String indicatorCode, IndicatorFilter filter) {
        if (!VALUE_CHECKABLE_OPERATORS.contains(filter.operator())) {
            return;
        }
        if (IndicatorCodes.SURVEY_YEAR_DIMENSION.equals(filter.dimension())) {
            return; // deliberately NOT checked against the member provider — see SurveyYearDimensionMemberProvider
        }
        memberProviders.find(filter.dimension()).ifPresent(provider -> {
            Set<String> validCodes = provider.validCodes(indicatorCode);
            for (Object value : filter.values()) {
                if (!validCodes.contains(String.valueOf(value))) {
                    throw new InvalidFilterValueException(filter.dimension(), value);
                }
            }
        });
    }

    /**
     * A {@code surveyYear} filter is only checked for being a well-formed year within the supported
     * span — never against the member list. A dashboard is configured with a fixed calendar range
     * (e.g. 2018–2026); the survey-stats-tool has data for only part of it, and a year it lacks must
     * degrade to {@code null} values downstream, not be rejected here. A non-integer or out-of-span
     * value is still a clean 400 so the handler's {@code Integer.parseInt} never sees garbage.
     */
    private List<Object> normalizeSurveyYearValues(IndicatorFilter filter) {
        List<Object> normalized = new ArrayList<>();
        int currentYear = Year.now().getValue();
        for (Object value : filter.values()) {
            int year;
            try {
                year = Integer.parseInt(String.valueOf(value).trim());
            } catch (NumberFormatException e) {
                throw new InvalidFilterValueException(filter.dimension(), value);
            }
            if (year < EARLIEST_SUPPORTED_YEAR || year > currentYear) {
                throw new InvalidFilterValueException(filter.dimension(), value);
            }
            normalized.add(Integer.toString(year));
        }
        return List.copyOf(normalized);
    }

    /**
     * An indicator that binds {@code surveyYear} as an <em>optional</em> filter (every
     * survey-stats-tool indicator does, since a {@code timeSeries} range is the alternative way to
     * pick years) needs one or the other — there is no "latest survey" default. Rejected here as a
     * 400 rather than left to blow up as a 500 inside the handler's {@code resolveYears}.
     */
    private void requireTimeScope(IndicatorDefinition definition, IndicatorQuery query, ResolvedTimeRange timeRange) {
        if (timeRange != null) {
            return;
        }
        boolean bindsOptionalSurveyYear = definition.dimensionBindings().stream()
            .anyMatch(binding -> IndicatorCodes.SURVEY_YEAR_DIMENSION.equals(binding.dimensionCode())
                && !binding.requiredFilter());
        if (!bindsOptionalSurveyYear) {
            return;
        }
        boolean hasSurveyYearFilter = query.filters().stream()
            .anyMatch(filter -> IndicatorCodes.SURVEY_YEAR_DIMENSION.equals(filter.dimension()));
        if (!hasSurveyYearFilter) {
            throw new MissingTimeScopeException(definition.code());
        }
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
