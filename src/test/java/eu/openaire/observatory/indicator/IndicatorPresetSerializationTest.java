package eu.openaire.observatory.indicator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Locks the {@code GET /indicators/presets} wire shape now that the record serializes directly (no DTO). */
class IndicatorPresetSerializationTest {

    private final IndicatorDefinitionCatalog definitionCatalog = new IndicatorDefinitionCatalog();
    private final IndicatorPresetCatalog catalog = new IndicatorPresetCatalog(definitionCatalog);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    @Test
    void serializesToTheExpectedFieldsWithEnumsAsWireStringsAndNoOrder() {
        JsonNode json = objectMapper.valueToTree(catalog.get("oa-1"));

        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
            "id", "label", "group", "chartType", "format",
            "allowedSeriesAggregations", "indicatorCode", "presetFilters");

        assertThat(json.get("id").asText()).isEqualTo("oa-1");
        assertThat(json.get("group").asText()).isEqualTo("Open Access Publications");
        assertThat(json.get("chartType").asText()).isEqualTo("SCALAR");
        assertThat(json.get("format").asText()).isEqualTo("percentage");
        assertThat(json.get("presetFilters").get("accessStatus").get(0).asText()).isEqualTo("OA");
        assertThat(json.has("order")).isFalse();
        assertThat(json.has("source")).isFalse();
    }

    @Test
    void everyPresetSerializesWithoutAnOrderKeyAndWithAResolvedFormat() {
        for (IndicatorPreset preset : catalog.getAll()) {
            JsonNode json = objectMapper.valueToTree(preset);
            assertThat(json.has("order")).as(preset.id() + " must not expose order").isFalse();
            assertThat(json.has("source")).as(preset.id() + " must not expose source").isFalse();
            assertThat(json.get("format").isTextual()).as(preset.id() + " format resolved").isTrue();
        }
    }

    @Test
    void yesNoFormatPresetSerializesFormatAsYesNo() {
        List<String> yesNoIds = catalog.getAll().stream()
            .filter(preset -> "yes-no".equals(objectMapper.valueToTree(preset).get("format").asText()))
            .map(IndicatorPreset::id)
            .toList();

        assertThat(yesNoIds).contains("oa-6"); // COUNTRY_INITIATIVE_STATUS -> yes-no
    }
}
