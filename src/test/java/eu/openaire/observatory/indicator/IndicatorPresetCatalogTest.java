package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorPresetCatalogTest {

    private final IndicatorDefinitionCatalog definitionCatalog = new IndicatorDefinitionCatalog();
    private final IndicatorPresetCatalog catalog = new IndicatorPresetCatalog(definitionCatalog);

    @Test
    void containsSixtySevenPresets() {
        assertThat(catalog.getAll()).hasSize(67);
    }

    @Test
    void everyIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (IndicatorPreset preset : catalog.getAll()) {
            assertThat(ids.add(preset.id())).as("duplicate id: " + preset.id()).isTrue();
        }
    }

    @Test
    void everyIndicatorCodeResolvesInTheRealCatalog() {
        for (IndicatorPreset preset : catalog.getAll()) {
            assertThat(definitionCatalog.get(preset.indicatorCode()))
                .as("preset indicator '" + preset.id() + "' references indicatorCode " + preset.indicatorCode())
                .isNotNull();
        }
    }

    @Test
    void everyPresetFilterNamesADimensionTheIndicatorActuallyBinds() {
        for (IndicatorPreset preset : catalog.getAll()) {
            IndicatorDefinition indicator = definitionCatalog.get(preset.indicatorCode());
            Set<String> boundDimensions = indicator.dimensionBindings().stream()
                .map(binding -> binding.dimensionCode())
                .collect(java.util.stream.Collectors.toSet());
            for (String presetDimension : preset.presetFilters().keySet()) {
                assertThat(boundDimensions)
                    .as("preset indicator '" + preset.id() + "' filters dimension '" + presetDimension
                        + "' which " + preset.indicatorCode() + " doesn't bind")
                    .contains(presetDimension);
            }
        }
    }

    @Test
    void getReturnsNullForUnknownId() {
        assertThat(catalog.get("does-not-exist")).isNull();
    }

    @Test
    void resolvesFormatFromEachPresetsDefinition() {
        for (IndicatorPreset preset : catalog.getAll()) {
            IndicatorDefinition definition = definitionCatalog.get(preset.indicatorCode());
            assertThat(preset.format())
                .as("format of preset indicator '" + preset.id() + "'")
                .isEqualTo(definition.format());
        }
    }

    @Test
    void presetIndicatorsAreReturnedInGroupOrder() {
        int previousGroupOrder = Integer.MIN_VALUE;
        for (IndicatorPreset presetIndicator : catalog.getAll()) {
            int groupOrder = presetIndicator.group().order();
            assertThat(groupOrder)
                .as("preset indicator '" + presetIndicator.id() + "' (" + presetIndicator.group() + ") breaks group order")
                .isGreaterThanOrEqualTo(previousGroupOrder);
            previousGroupOrder = groupOrder;
        }
    }

    @Test
    void presetIndicatorsWithinAGroupAreOrdered() {
        IndicatorGroup currentGroup = null;
        int previousOrder = Integer.MIN_VALUE;
        for (IndicatorPreset presetIndicator : catalog.getAll()) {
            if (presetIndicator.group() != currentGroup) {
                currentGroup = presetIndicator.group();
                previousOrder = Integer.MIN_VALUE;
            }
            assertThat(presetIndicator.order())
                .as("preset indicator '" + presetIndicator.id() + "' breaks order within " + currentGroup)
                .isGreaterThan(previousOrder);
            previousOrder = presetIndicator.order();
        }
    }

    @Test
    void firstAndLastPresetIndicatorsAreStable() {
        List<IndicatorPreset> all = catalog.getAll();
        assertThat(all.getFirst().id()).isEqualTo("oa-1");
        assertThat(all.getLast().id()).isEqualTo("fin-1");
    }
}
