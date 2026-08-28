package eu.openaire.observatory.indicator.model;

/**
 * The canonical palette of chart/render styles the observatory UI can draw for a preset view.
 * A {@link PresetView} pairs one of these with a {@link CountryScope}/{@link TimeScope} cell;
 * the UI renders the style of whichever view matches the actor's current filter state.
 *
 * <p>This is the full set the UI supports, not only the styles referenced by today's catalog —
 * the unused members are wired up as later indicator groups (and the full-width "Distribution
 * of Open Access Types" indicator) are added.
 *
 * <p>A breakdown dropdown (e.g. "by access type") is <em>not</em> a render style — it is expressed
 * by a {@link PresetView#selector()}, layered on any of these styles.
 */
public enum RenderStyle {

    /**
     * A single aggregate value (the UI's "percentage" card); the unit/suffix — %, € millions,
     * a plain count — comes from the preset's {@code format}, not from this style.
     */
    SCALAR,

    /** Single-series line over the selected year range. */
    LINE_CHART,

    /** One bar per country with the value printed on the right. */
    PROGRESS_BARS,

    /** One line series per selected country over the year range. */
    MULTI_SERIES_LINE_CHART,

    /** Per-country yes/no for a single year. */
    YES_NO,

    /** Per-country yes/no plus when each country changed, if it did, across the range. */
    YES_NO_TIMELINE,

    /** Per-country choropleth for a single year. */
    MAP,

    /** Per-country choropleth plus a count of how many countries changed since {@code yearFrom}. */
    MAP_WITH_CHANGE_COUNT,

    /** Stacked column, as rendered on the Explore dashboard. */
    STACKED_COLUMN,

    /** Progress line chart across several countries. */
    PROGRESS_LINE_CHART,

    /** Small-multiples line charts. */
    MULTI_LINE_CHARTS,

    /** Choropleth map highlighting the most active countries. */
    CHOROPLETH_MAP_MOST_ACTIVE_COUNTRIES,

    /** Choropleth map, as already implemented in the EOSC Observatory. */
    CHOROPLETH_MAP,

    /** Single-series column chart. */
    COLUMN_CHART,

    /** Column chart with multiple data series. */
    MULTI_SERIES_COLUMN_CHART
}
