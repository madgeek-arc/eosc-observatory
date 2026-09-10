package eu.openaire.observatory.indicator;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresetValueLabelsTest {

    @Test
    void noneIsEmptyAndCarriesNoLabels() {
        assertThat(PresetValueLabels.NONE.isEmpty()).isTrue();
        assertThat(PresetValueLabels.NONE.values()).isEmpty();
        assertThat(PresetValueLabels.NONE.missingValue()).isNull();
    }

    @Test
    void forMissingRelabelsOnlyTheGapFilledValue() {
        PresetValueLabels labels = PresetValueLabels.forMissing("N/A");

        assertThat(labels.isEmpty()).isFalse();
        assertThat(labels.values()).isEmpty();
        assertThat(labels.missingValue()).isEqualTo("N/A");
    }

    @Test
    void nullValueMapIsNormalisedToEmpty() {
        assertThat(new PresetValueLabels(null, "N/A").values()).isEmpty();
    }

    @Test
    void rejectsABlankLabel() {
        assertThatThrownBy(() -> new PresetValueLabels(Map.of("YES", " "), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank entry");
    }

    @Test
    void rejectsABlankRawKey() {
        assertThatThrownBy(() -> new PresetValueLabels(Map.of("", "YES"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank entry");
    }

    @Test
    void rejectsANullLabel() {
        Map<String, String> withNull = new HashMap<>();
        withNull.put("YES", null);

        assertThatThrownBy(() -> new PresetValueLabels(withNull, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("blank entry");
    }

    @Test
    void rejectsABlankMissingValue() {
        assertThatThrownBy(() -> new PresetValueLabels(Map.of(), "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("missingValue is blank");
    }

    @Test
    void copiesTheGivenMapDefensively() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("YES", "YES");
        PresetValueLabels labels = new PresetValueLabels(mutable, null);

        mutable.put("NO", "NO");

        assertThat(labels.values()).containsExactly(org.assertj.core.api.Assertions.entry("YES", "YES"));
    }
}
