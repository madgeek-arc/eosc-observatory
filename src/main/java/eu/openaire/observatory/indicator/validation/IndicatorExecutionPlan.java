package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;

import java.util.List;

/**
 * The resolved/validated shape of an IndicatorQuery, checked against its IndicatorDefinition.
 * Handlers receive this, never the raw query.
 */
public record IndicatorExecutionPlan(
    IndicatorDefinition definition,
    AggregationType aggregation,
    List<ResolvedFilter> filters,
    List<ResolvedDimension> groupBy,
    ResolvedTimeRange timeRange, // nullable
    QuerySecurityScope securityScope, // trusted, server-derived; never from the client request
    int limit
) {
}
