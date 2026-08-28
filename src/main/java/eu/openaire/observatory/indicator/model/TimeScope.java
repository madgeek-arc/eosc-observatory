package eu.openaire.observatory.indicator.model;

/**
 * Whether a {@link eu.openaire.observatory.indicator.model.PresetView} applies when the actor
 * picked a single year ({@link #SINGLE_YEAR}, {@code yearFrom == yearTo}) or a multi-year span
 * ({@link #TIME_RANGE}). One of the two axes the UI keys on when choosing which view of a
 * preset to render.
 */
public enum TimeScope {
    SINGLE_YEAR,
    TIME_RANGE
}
