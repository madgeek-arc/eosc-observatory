package eu.openaire.observatory.indicator.model;

/**
 * Query-time aggregation of already-computed indicator values only (e.g. summing per-country
 * publication counts into a total). How a handler derives its base measure — including any
 * COUNT/COUNT_DISTINCT it performs internally — is a handler concern, not a user-selectable
 * aggregation, and so has no member here.
 *
 * <p>Single-series aggregations only. Ratio is not a member here: it is triggered by
 * {@link IndicatorSemanticType#RATIO} and handled entirely by that indicator's own execution
 * binding — see {@link RatioPolicy}.
 */
public enum AggregationType {
    SUM,
    AVG,
    MIN,
    MAX,
    FIRST,
    LAST,
    NONE
}
