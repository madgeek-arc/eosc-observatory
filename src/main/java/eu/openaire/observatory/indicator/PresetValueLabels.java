package eu.openaire.observatory.indicator;

import java.util.Map;

/**
 * The display strings a preset's query results carry, so the UI can use the values straight as
 * chart legend labels. {@code values} maps a raw data-point value to its label (a status code like
 * {@code "AWAITING_DATA"} to {@code "N/A"}); {@code missingValue} is the label a gap-filled
 * {@code null} year renders as. Applied to the {@code POST /indicators/presets/{id}/query} response
 * by {@link PresetValueLabeler}; the generic {@code POST /indicators/query} endpoint is untouched.
 *
 * <p>Authored via {@link IndicatorPreset#withValueLabels(PresetValueLabels)}; {@link #NONE} for
 * every preset whose raw values are already display-ready. Whether {@code values} covers every
 * value a handler can emit is not enforced — unmapped values pass through unchanged.
 */
public record PresetValueLabels(Map<String, String> values, String missingValue) {

    /** No relabeling — raw values reach the wire as-is. */
    public static final PresetValueLabels NONE = new PresetValueLabels(Map.of(), null);

    public PresetValueLabels {
        Map<String, String> given = values != null ? values : Map.of();
        given.forEach((raw, label) -> {
            if (raw == null || raw.isBlank() || label == null || label.isBlank()) {
                throw new IllegalArgumentException(
                    "PresetValueLabels has a blank entry: '" + raw + "' -> '" + label + "'");
            }
        });
        if (missingValue != null && missingValue.isBlank()) {
            throw new IllegalArgumentException("PresetValueLabels.missingValue is blank");
        }
        values = Map.copyOf(given);
    }

    /** Relabel only the gap-filled {@code null} value; leave every raw value as-is. */
    public static PresetValueLabels forMissing(String missingValue) {
        return new PresetValueLabels(Map.of(), missingValue);
    }

    /** True when there is nothing to relabel — {@link PresetValueLabeler} then returns the result untouched. */
    public boolean isEmpty() {
        return values.isEmpty() && missingValue == null;
    }
}
