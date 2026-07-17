package eu.openaire.observatory.indicator.model;

/**
 * Where/how an indicator's value is actually computed — orthogonal to {@link IndicatorSemanticType}.
 * A single handler (e.g. one external service client) typically serves many indicators, each
 * distinguished by its own providerIndicatorKey.
 */
public record IndicatorExecutionBinding(
    String handlerKey, // routes to an IndicatorQueryHandler in the registry
    String providerIndicatorKey // this indicator's identifier within that handler's source
) {
}
