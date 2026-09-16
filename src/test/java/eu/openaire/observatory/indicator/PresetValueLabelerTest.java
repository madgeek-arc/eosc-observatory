package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.result.IndicatorDataPoint;
import eu.openaire.observatory.indicator.result.IndicatorMetadata;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.result.QueryExecutionMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PresetValueLabelerTest {

    private final PresetValueLabeler labeler = new PresetValueLabeler();

    private static IndicatorPreset presetWithLabels(Map<String, String> valueLabels, String missingValueLabel) {
        return new IndicatorPreset(
            "oa-8", "National Monitoring", IndicatorGroup.OPEN_ACCESS_PUBLICATIONS,
            List.of(), null, List.of(), "oa.country_initiative_status", Map.of(),
            new PresetValueLabels(valueLabels, missingValueLabel), 80);
    }

    private static IndicatorResult resultOf(IndicatorDataPoint... data) {
        return new IndicatorResult(
            new IndicatorMetadata("oa.country_initiative_status", "National Monitoring", null, null, null),
            List.of("country"), List.of(data),
            new QueryExecutionMetadata(Instant.EPOCH, false));
    }

    private static IndicatorDataPoint point(String country, Object value) {
        return new IndicatorDataPoint(Map.of("country", country), value);
    }

    private final Map<String, String> monitoringLabels = Map.of(
        "YES", "Has national monitoring",
        "NO", "Does not have national monitoring",
        "AWAITING_DATA", "N/A");

    @Test
    void relabelsRawStringValuesThroughTheValueLabelMap() {
        IndicatorResult relabeled = labeler.relabel(
            presetWithLabels(monitoringLabels, "N/A"),
            resultOf(point("GR", "YES"), point("IT", "NO"), point("FR", "AWAITING_DATA")));

        assertThat(relabeled.data()).extracting(IndicatorDataPoint::value)
            .containsExactly("Has national monitoring", "Does not have national monitoring", "N/A");
        assertThat(relabeled.data()).extracting(p -> p.dimensions().get("country"))
            .containsExactly("GR", "IT", "FR");
    }

    @Test
    void relabelsNullGapFilledValuesToTheMissingValueLabel() {
        IndicatorResult relabeled = labeler.relabel(
            presetWithLabels(monitoringLabels, "N/A"),
            resultOf(point("GR", "YES"), point("GR", null)));

        assertThat(relabeled.data()).extracting(IndicatorDataPoint::value)
            .containsExactly("Has national monitoring", "N/A");
    }

    @Test
    void leavesNullAsIsWhenNoMissingValueLabelIsDeclared() {
        IndicatorResult relabeled = labeler.relabel(
            presetWithLabels(monitoringLabels, null),
            resultOf(point("GR", null)));

        assertThat(relabeled.data().get(0).value()).isNull();
    }

    @Test
    void leavesUnmappedStringValuesUnchanged() {
        IndicatorResult relabeled = labeler.relabel(
            presetWithLabels(monitoringLabels, "N/A"),
            resultOf(point("GR", "SOMETHING_ELSE")));

        assertThat(relabeled.data().get(0).value()).isEqualTo("SOMETHING_ELSE");
    }

    @Test
    void leavesNonStringValuesUnchanged() {
        IndicatorResult relabeled = labeler.relabel(
            presetWithLabels(Map.of("YES", "Has national monitoring"), "N/A"),
            resultOf(point("GR", 42)));

        assertThat(relabeled.data().get(0).value()).isEqualTo(42);
    }

    @Test
    void returnsTheSameResultInstanceWhenThePresetDeclaresNoLabels() {
        IndicatorPreset noLabels = new IndicatorPreset(
            "oa-1", "OA", IndicatorGroup.OPEN_ACCESS_PUBLICATIONS,
            List.of(), null, List.of(), "publications.access_status_share", Map.of(),
            PresetValueLabels.NONE, 10);
        IndicatorResult original = resultOf(point("GR", "YES"));

        assertThat(labeler.relabel(noLabels, original)).isSameAs(original);
    }

    @Test
    void relabelsSummaryPointsToo() {
        IndicatorResult withSummary = new IndicatorResult(
            new IndicatorMetadata("oa.country_initiative_status", "National Monitoring", null, null, null),
            List.of("country"), List.of(point("GR", "YES")),
            new QueryExecutionMetadata(Instant.EPOCH, false), List.of(point("GR", "NO")));

        IndicatorResult relabeled = labeler.relabel(presetWithLabels(monitoringLabels, "N/A"), withSummary);

        assertThat(relabeled.summary().get(0).value()).isEqualTo("Does not have national monitoring");
    }
}
