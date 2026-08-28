package eu.openaire.observatory.indicator.model;

/**
 * Whether a {@link eu.openaire.observatory.indicator.model.PresetView} applies when the actor
 * looks at every country ({@link #ALL_COUNTRIES}, an EU-wide aggregate) or at a hand-picked
 * subset ({@link #SELECTED_COUNTRIES}, one row per country). One of the two axes the UI keys
 * on when choosing which view of a preset to render.
 */
public enum CountryScope {
    ALL_COUNTRIES,
    SELECTED_COUNTRIES
}
