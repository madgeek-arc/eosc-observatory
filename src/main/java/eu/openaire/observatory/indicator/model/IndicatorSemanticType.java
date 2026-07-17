package eu.openaire.observatory.indicator.model;

/**
 * What an indicator's value *means*, independent of where or how it is computed. Sourcing
 * (local vs. external, cached, materialized, etc.) is described by {@link IndicatorExecutionBinding}
 * instead — a remotely computed indicator can still be a MEASURE, a RATIO, or an ATTRIBUTE.
 */
public enum IndicatorSemanticType {

    /** A summable/countable numeric quantity, e.g. publication count or funding amount. */
    MEASURE,

    /** Numerator divided by denominator; never averaged directly across groups. */
    RATIO,

    /** Non-numeric categorical value. */
    ATTRIBUTE
}
