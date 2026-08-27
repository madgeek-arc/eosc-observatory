package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorCatalogTest {

    private final IndicatorDefinitionCatalog definitionCatalog = new IndicatorDefinitionCatalog();
    private final IndicatorCatalog catalog = new IndicatorCatalog(definitionCatalog);

    @Test
    void containsSixtySevenPresets() {
        assertThat(catalog.getAll()).hasSize(67);
    }

    @Test
    void everyIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (Indicator preset : catalog.getAll()) {
            assertThat(ids.add(preset.id())).as("duplicate id: " + preset.id()).isTrue();
        }
    }

    @Test
    void everyIndicatorCodeResolvesInTheRealCatalog() {
        for (Indicator preset : catalog.getAll()) {
            assertThat(definitionCatalog.get(preset.indicatorCode()))
                .as("preset '" + preset.id() + "' references indicatorCode " + preset.indicatorCode())
                .isNotNull();
        }
    }

    @Test
    void everyPresetFilterNamesADimensionTheIndicatorActuallyBinds() {
        for (Indicator preset : catalog.getAll()) {
            IndicatorDefinition indicator = definitionCatalog.get(preset.indicatorCode());
            Set<String> boundDimensions = indicator.dimensionBindings().stream()
                .map(binding -> binding.dimensionCode())
                .collect(java.util.stream.Collectors.toSet());
            for (String presetDimension : preset.presetFilters().keySet()) {
                assertThat(boundDimensions)
                    .as("preset '" + preset.id() + "' filters dimension '" + presetDimension
                        + "' which " + preset.indicatorCode() + " doesn't bind")
                    .contains(presetDimension);
            }
        }
    }

    @Test
    void getReturnsNullForUnknownId() {
        assertThat(catalog.get("does-not-exist")).isNull();
    }
}
