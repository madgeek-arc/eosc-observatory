package eu.openaire.observatory.indicator.model;

public enum IndicatorKind {

    /** Additive event or entity count, e.g. number of publications. */
    COUNT,

    /** A summable quantity, e.g. research funding amount. */
    MEASURE,

    /** A point-in-time value, e.g. active researchers on Dec 31. */
    SNAPSHOT,

    /** Numerator divided by denominator; never averaged directly. */
    RATIO,

    /** Computed externally; this app only stores/serves the value. */
    PRECOMPUTED,

    /** Non-numeric categorical value. */
    ATTRIBUTE
}
