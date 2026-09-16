package eu.openaire.observatory.indicator.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

/**
 * One selectable rendering of a preset indicator: the {@link RenderStyle} to draw when the
 * actor's filters fall in the {@code countryScope}/{@code timeScope} cell.
 *
 * <p>A preset carries a list of these (see {@link eu.openaire.observatory.indicator.IndicatorPreset#views()}),
 * normally one per cell of the 2×2. Rendering stays entirely a UI decision — the backend only
 * publishes which styles make sense; nothing here is persisted on a saved dashboard.
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
 */
public record PresetView(
    CountryScope countryScope,
    TimeScope timeScope,
    RenderStyle renderStyle,
    @JsonInclude(JsonInclude.Include.NON_NULL) ViewSelector selector,
    boolean fullWidth
) {
    public PresetView {
        Objects.requireNonNull(renderStyle, "renderStyle cannot be null");
    }

    /** Most views have no selector and aren't full-width. */
    public PresetView(CountryScope countryScope, TimeScope timeScope, RenderStyle renderStyle) {
        this(countryScope, timeScope, renderStyle, null, false);
    }

    /** Most views aren't full-width. */
    public PresetView(CountryScope countryScope, TimeScope timeScope, RenderStyle renderStyle, ViewSelector selector) {
        this(countryScope, timeScope, renderStyle, selector, false);
    }

    /** A copy with {@code fullWidth} set — used by the catalog to mark every view of a full-width preset. */
    public PresetView withFullWidth(boolean fullWidth) {
        return new PresetView(countryScope, timeScope, renderStyle, selector, fullWidth);
    }
}
