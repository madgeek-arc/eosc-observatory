package eu.openaire.observatory.indicator.model;

/**
 * Only meaningful when {@code IndicatorDefinition.semanticType() == RATIO}. Generic numerator/
 * denominator orchestration across handlers is deferred — for this slice the ratio's own handler
 * (via its IndicatorExecutionBinding) is fully responsible for computing the value; this policy
 * only governs the one behavior the validator/normalizer need to know about up front.
 */
public record RatioPolicy(
    ZeroDenominatorHandling zeroDenominatorHandling
) {
}
