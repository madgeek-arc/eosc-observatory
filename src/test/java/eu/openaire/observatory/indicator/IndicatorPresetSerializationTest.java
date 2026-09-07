package eu.openaire.observatory.indicator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.indicator.model.CountryScope;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.model.RenderStyle;
import eu.openaire.observatory.indicator.model.TimeScope;
import eu.openaire.observatory.indicator.model.ViewSelector;
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
            "id", "label", "group", "views", "fullWidth", "format",
            "allowedSeriesAggregations", "indicatorCode", "presetFilters");

        assertThat(json.get("id").asText()).isEqualTo("oa-1");
        assertThat(json.get("group").asText()).isEqualTo("Open Access Publications");
        assertThat(json.get("format").asText()).isEqualTo("percentage");
        assertThat(json.get("presetFilters").get("accessStatus").get(0).asText()).isEqualTo("OA");
        assertThat(json.has("order")).isFalse();
        assertThat(json.has("source")).isFalse();
        assertThat(json.has("chartType")).isFalse();
    }

    @Test
    void viewsSerializeAsTaggedRenderStyleEntries() {
        JsonNode views = objectMapper.valueToTree(catalog.get("oa-1")).get("views");

        assertThat(views.isArray()).isTrue();
        assertThat(views).hasSize(4);
        views.forEach(view -> assertThat(fieldNames(view))
            .containsExactlyInAnyOrder("countryScope", "timeScope", "renderStyle"));

        JsonNode allCountriesSingleYear = findView(views, "ALL_COUNTRIES", "SINGLE_YEAR");
        assertThat(allCountriesSingleYear.get("renderStyle").asText()).isEqualTo("SCALAR");
        assertThat(findView(views, "SELECTED_COUNTRIES", "SINGLE_YEAR").get("renderStyle").asText())
            .isEqualTo("PROGRESS_BARS");
    }

    @Test
    void viewWithoutASelectorOmitsTheKey() {
        JsonNode view = objectMapper.valueToTree(
            new PresetView(CountryScope.ALL_COUNTRIES, TimeScope.SINGLE_YEAR, RenderStyle.SCALAR));

        assertThat(fieldNames(view)).containsExactlyInAnyOrder("countryScope", "timeScope", "renderStyle");
    }

    @Test
    void viewSelectorSerializesWithItsDimensionAndMembers() {
        JsonNode view = objectMapper.valueToTree(new PresetView(
            CountryScope.SELECTED_COUNTRIES, TimeScope.TIME_RANGE, RenderStyle.MULTI_LINE_CHARTS,
            new ViewSelector("accessStatus", null, List.of("OA", "CLOSED"))));

        JsonNode selector = view.get("selector");
        assertThat(selector).isNotNull();
        assertThat(selector.get("dimension").asText()).isEqualTo("accessStatus");
        assertThat(selector.get("label").isNull()).isTrue();
        assertThat(selector.get("members")).hasSize(2);
        assertThat(selector.get("members").get(0).asText()).isEqualTo("OA");
    }

    private static JsonNode findView(JsonNode views, String countryScope, String timeScope) {
        for (JsonNode view : views) {
            if (countryScope.equals(view.get("countryScope").asText())
                && timeScope.equals(view.get("timeScope").asText())) {
                return view;
            }
        }
        throw new AssertionError("no view for " + countryScope + "/" + timeScope);
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

        assertThat(yesNoIds).contains("oa-8"); // COUNTRY_INITIATIVE_STATUS -> yes-no
    }
}
