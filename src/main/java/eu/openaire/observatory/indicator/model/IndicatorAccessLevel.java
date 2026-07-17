package eu.openaire.observatory.indicator.model;

/** Minimal access gate for the first slice — no per-role permission table yet. */
public enum IndicatorAccessLevel {
    PUBLIC,
    AUTHENTICATED,
    ORGANIZATION,
    RESTRICTED
}
