package eu.openaire.observatory.indicator.query;

public enum ComparisonMode {
    /** Diffs exactly the timeSeries range's endpoints (from vs to), ignoring any years in between. */
    TWO_POINT,

    /** Diffs every adjacent year pair across the full timeSeries range (year-over-year). */
    CONSECUTIVE
}
