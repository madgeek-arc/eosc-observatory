package eu.openaire.observatory.indicator.predefined;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PreDefinedIndicatorCatalogTest {

    private final IndicatorCatalog indicatorCatalog = new IndicatorCatalog();
    private final PreDefinedIndicatorCatalog catalog = new PreDefinedIndicatorCatalog(indicatorCatalog);

    @Test
    void containsSixtySevenWidgets() {
        assertThat(catalog.getAll()).hasSize(67);
    }

    @Test
    void everyIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (PreDefinedIndicatorDefinition definition : catalog.getAll()) {
            assertThat(ids.add(definition.id())).as("duplicate id: " + definition.id()).isTrue();
        }
    }

    @Test
    void everyIndicatorCodeResolvesInTheRealCatalog() {
        for (PreDefinedIndicatorDefinition definition : catalog.getAll()) {
            assertThat(indicatorCatalog.get(definition.indicatorCode()))
                .as("widget '" + definition.id() + "' references indicatorCode " + definition.indicatorCode())
                .isNotNull();
        }
    }

    @Test
    void everyPresetFilterNamesADimensionTheIndicatorActuallyBinds() {
        for (PreDefinedIndicatorDefinition definition : catalog.getAll()) {
            IndicatorDefinition indicator = indicatorCatalog.get(definition.indicatorCode());
            Set<String> boundDimensions = indicator.dimensionBindings().stream()
                .map(binding -> binding.dimensionCode())
                .collect(java.util.stream.Collectors.toSet());
            for (String presetDimension : definition.presetFilters().keySet()) {
                assertThat(boundDimensions)
                    .as("widget '" + definition.id() + "' presets dimension '" + presetDimension
                        + "' which " + definition.indicatorCode() + " doesn't bind")
                    .contains(presetDimension);
            }
        }
    }

    @Test
    void getReturnsNullForUnknownId() {
        assertThat(catalog.get("does-not-exist")).isNull();
    }
}
