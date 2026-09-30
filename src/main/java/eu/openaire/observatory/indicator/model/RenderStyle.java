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

    /** One bar per country with the value printed on the right. */
    PROGRESS_BARS_NUM_PERCENTAGE,

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

    /** Choropleth map, as already implemented in the EOSC Observatory. */
    CHOROPLETH_MAP,

    /** Choropleth map highlighting the top 5 countries. */
    CHOROPLETH_MAP_WITH_TOP_5,

    /** Single-series column chart. */
    COLUMN_CHART,

    /** Column chart with multiple data series. */
    MULTI_SERIES_COLUMN_CHART,

    /** Single-series bar chart — one bar per area/category, for a single year. */
    BAR_CHART,

    /** Bar chart with one series per year across the selected range. */
    MULTI_SERIES_BAR_CHART,

    /** Table-like grid of per-country yes/no across the areas, for a single year. */
    YES_NO_TABLE,

    /**
     * Composite: the year each country adopted the initiative, paired with a coverage-over-time
     * trend — the status in the {@code to} year shown alongside.
     */
    YEAR_ADOPTED_TABLE,

    /** The coverage-over-time trend panel on its own (the "only the coverage trend view" variant). */
    COVERAGE_TREND,

    /**
     * Table-like grid of per-country yes/no across the areas, with year as an added axis (one
     * section per year in the selected range) instead of a single year.
     */
    YEAR_COUNTRY_AREA_TABLE,

    /** {@link #YES_NO_TABLE}, computed from the same time-range response but showing only its most recent year. */
    LATEST_YEAR_YES_NO_TABLE,

    /**
     * Per-area count of the countries answering Yes, with the average across areas called out on
     * top, for a single year.
     */
    COUNT_BY_AREA_WITH_AVERAGE,

    /** Column chart with the percentage/number printed on top of each column, across the year range. */
    COLUMN_CHART_WITH_VALUE_LABELS,

    /**
     * A stacked bar with a progress bar per category beneath it and the value/percentage on the
     * right, for a single year.
     */
    STACKED_BAR_WITH_PROGRESS,

    /** One stacked bar per country, for a single year. */
    MULTI_STACKED_BARS,

    /**
     * One stacked column chart per country, across the year range — {@link #MULTI_STACKED_BARS}'s
     * time-range counterpart. Deliberately distinct from {@link #STACKED_COLUMN}: unlike that
     * style, this one does not trigger {@code groupByFor}'s classification add-on, since here the
     * stack is the selector's dimension, not a classification breakdown.
     */
    MULTI_STACKED_COLUMNS,

    /**
     * Pie chart paired with a companion list of progress bars carrying both the percentages and the
     * absolute numbers, for a single year.
     */
    PIE_WITH_PROGRESS_BARS,

    /**
     * Composite: a per-year / cumulative toggle over multi-series line charts.
     */
    COMPOSITE_PER_YEAR_AND_CUMULATIVE_LINES,

    /**
     * Composite: an "amount" and a "share" view, each a column chart with the number printed on top.
     */
    COMPOSITE_AMOUNT_AND_SHARE_COLUMNS,

    /**
     * Composite: a "composition" view (multiple column charts) and a "per area" view (a tab per
     * area, each with multi-series line charts).
     */
    COMPOSITE_COMPOSITION_AND_PER_AREA;

    /**
     * Whether drawing this style needs one value per country. It's what decides whether a preset
     * query for the view matching the actor's {@code (countryScope, timeScope)} cell adds
     * {@code groupBy: ["country"]} — maps, choropleths, progress bars, per-country yes/no and
     * multi-series charts all need per-country rows; {@code SCALAR} / {@code LINE_CHART} want a
     * single aggregate (for RATIO indicators the true EU value can't be recovered from per-country
     * rows, so it must be computed without the grouping).
     *
     * <p>{@code COLUMN_CHART} counts as per-country — {@code OUTPUT_COUNT_VIEWS} uses it for
     * "most-active-countries" charts. {@code STACKED_COLUMN} is category-over-time, not per-country;
     * so are the EU-aggregate / by-area styles ({@code BAR_CHART}, {@code STACKED_BAR_WITH_PROGRESS},
     * {@code PIE_WITH_PROGRESS_BARS}, {@code COUNT_BY_AREA_WITH_AVERAGE}, …) that draw one mark per
     * area or category rather than one per country.
     */
    public boolean perCountryBreakdown() {
        return switch (this) {
            case SCALAR, LINE_CHART, STACKED_COLUMN,
                 BAR_CHART, MULTI_SERIES_BAR_CHART, COUNT_BY_AREA_WITH_AVERAGE,
                 COLUMN_CHART_WITH_VALUE_LABELS, STACKED_BAR_WITH_PROGRESS, PIE_WITH_PROGRESS_BARS,
                 COMPOSITE_AMOUNT_AND_SHARE_COLUMNS -> false;
            default -> true;
        };
    }
}
