package eu.openaire.observatory.indicator.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One entry in a {@link PresetView#renderStyles()} switcher: the {@link RenderStyle} to draw,
 * paired with the caption the UI shows for it (a tab/button label). {@code label} is required
 * whenever a view offers more than one style; a view with a single style may leave it
 * {@code null} — there's nothing to switch between, so no caption is needed.
 */
public record RenderStyleOption(
    RenderStyle style,
    @JsonInclude(JsonInclude.Include.NON_NULL) String label
) {
    public RenderStyleOption {
        if (style == null) {
            throw new IllegalArgumentException("RenderStyleOption.style cannot be null");
        }
    }
}
