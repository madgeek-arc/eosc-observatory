package eu.openaire.observatory.indicator.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * One or more selectable renderings of a preset indicator for the {@code countryScope}/{@code
 * timeScope} cell the actor's filters fall into.
 *
 * <p>A preset carries a list of these (see {@link eu.openaire.observatory.indicator.IndicatorPreset#views()}),
 * normally one per cell of the 2×2. Rendering stays entirely a UI decision — the backend only
 * publishes which styles make sense; nothing here is persisted on a saved dashboard.
 *
 * <p>{@code renderStyles} is usually a single entry. When it carries more than one, every entry
 * draws the *same* query response — switching styles never re-queries (see
 * {@link eu.openaire.observatory.indicator.IndicatorPreset#toIndicatorQuery}, which derives
 * {@code groupBy} from the cell, not from which style is on screen) — and the UI offers a
 * switcher between them, captioned with each entry's {@link RenderStyleOption#label()}.
 *
 * <p>A {@code null} on either axis matches both values of that axis.
 *
 * <p>{@code selector} is optional: when set, this view offers a breakdown dropdown over one
 * dimension (see {@link ViewSelector}). A {@code null} selector is omitted from the wire.
 *
 * <p>{@code fullWidth} is layout metadata only — {@code true} when this specific rendering needs
 * the whole dashboard width rather than sharing a row with another widget; {@code false} for
 * every other view. Most presets set it the same on every one of their views, but nothing
 * requires that — a preset can mix full-width and non-full-width views.
 *
 * <p>{@code viewQuery} is optional: when set, this cell runs that {@link ViewQuery} (its own
 * indicator, pinned filters and {@code groupBy}) instead of the preset's, and carries no
 * {@code selector}. A {@code null} viewQuery is omitted from the wire.
 */
public record PresetView(
    CountryScope countryScope,
    TimeScope timeScope,
    List<RenderStyleOption> renderStyles,
    @JsonInclude(JsonInclude.Include.NON_NULL) ViewSelector selector,
    boolean fullWidth,
    @JsonInclude(JsonInclude.Include.NON_NULL) ViewQuery viewQuery
) {
    public PresetView {
        if (renderStyles == null || renderStyles.isEmpty()) {
            throw new IllegalArgumentException("renderStyles cannot be empty");
        }
        renderStyles = List.copyOf(renderStyles);
    }

    /** A view with its own render styles and selector that runs its preset's query. */
    public PresetView(CountryScope countryScope, TimeScope timeScope, List<RenderStyleOption> renderStyles,
                      ViewSelector selector, boolean fullWidth) {
        this(countryScope, timeScope, renderStyles, selector, fullWidth, null);
    }

    /** A single-style view with no selector, not full-width — the common case. */
    public PresetView(CountryScope countryScope, TimeScope timeScope, RenderStyle renderStyle) {
        this(countryScope, timeScope, List.of(new RenderStyleOption(renderStyle, null)), null, false);
    }

    /** A single-style view with a selector, not full-width. */
    public PresetView(CountryScope countryScope, TimeScope timeScope, RenderStyle renderStyle, ViewSelector selector) {
        this(countryScope, timeScope, List.of(new RenderStyleOption(renderStyle, null)), selector, false);
    }

    /** A copy with {@code fullWidth} set — used by the catalog to mark every view of a full-width preset. */
    public PresetView withFullWidth(boolean fullWidth) {
        return new PresetView(countryScope, timeScope, renderStyles, selector, fullWidth, viewQuery);
    }

    /** A copy carrying a different set of switchable render styles — the cell and selector stay the same. */
    public PresetView withRenderStyles(List<RenderStyleOption> renderStyles) {
        return new PresetView(countryScope, timeScope, renderStyles, selector, fullWidth, viewQuery);
    }

    /** A copy that runs {@code viewQuery} instead of its preset's query; the selector is dropped. */
    public PresetView withViewQuery(ViewQuery viewQuery) {
        return new PresetView(countryScope, timeScope, renderStyles, null, fullWidth, viewQuery);
    }

    /** The default style — the first entry of {@link #renderStyles()}, drawn before the actor picks a switcher tab. */
    public RenderStyle renderStyle() {
        return renderStyles.get(0).style();
    }
}
