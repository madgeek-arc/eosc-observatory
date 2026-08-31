package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.model.PresetView;
import eu.openaire.observatory.indicator.query.IndicatorFilter;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.query.TimeSeriesRequest;
import eu.openaire.observatory.indicator.validation.SeriesAggregationNotAllowedException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void everyPresetHasAFullTwoByTwoOfViews() {
        for (IndicatorPreset preset : catalog.getAll()) {
            assertThat(preset.views())
                .as("views of preset indicator '" + preset.id() + "'")
                .hasSize(4);
            Set<String> cells = new HashSet<>();
            for (PresetView view : preset.views()) {
                assertThat(view.renderStyle()).as(preset.id() + " view render style").isNotNull();
                assertThat(cells.add(view.countryScope() + "/" + view.timeScope()))
                    .as("preset indicator '" + preset.id() + "' repeats the "
                        + view.countryScope() + "/" + view.timeScope() + " cell")
                    .isTrue();
            }
        }
    }

    @Test
    void everyViewSelectorNamesADimensionTheIndicatorCanGroupBy() {
        for (IndicatorPreset preset : catalog.getAll()) {
            IndicatorDefinition indicator = definitionCatalog.get(preset.indicatorCode());
            for (PresetView view : preset.views()) {
                if (view.selector() == null) {
                    continue;
                }
                boolean groupable = indicator.dimensionBindings().stream()
                    .anyMatch(binding -> binding.dimensionCode().equals(view.selector().dimension())
                        && binding.usages().contains(DimensionUsage.GROUP));
                assertThat(groupable)
                    .as("preset indicator '" + preset.id() + "' has a selector on '"
                        + view.selector().dimension() + "' which " + preset.indicatorCode()
                        + " can't group by")
                    .isTrue();
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

    @Test
    void toIndicatorQueryBuildsACountryFilterFromSelectedCountries() {
        IndicatorQuery query = catalog.toIndicatorQuery(
            catalog.get("oa-1"), List.of("EL", "IT"), 2019, 2023, List.of());

        assertThat(query.filters())
            .contains(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("EL", "IT")));
    }

    @Test
    void toIndicatorQueryTurnsTheYearRangeIntoATimeSeriesForATrendCapablePreset() {
        IndicatorQuery query = catalog.toIndicatorQuery(
            catalog.get("oa-1"), List.of(), 2019, 2023, List.of());

        assertThat(query.timeSeries()).isEqualTo(new TimeSeriesRequest(2019, 2023, null));
    }

    @Test
    void toIndicatorQueryTurnsYearToIntoASurveyYearFilterForASurveyPreset() {
        IndicatorQuery query = catalog.toIndicatorQuery(
            catalog.get("oa-6"), List.of(), 2019, 2023, List.of());

        assertThat(query.timeSeries()).isNull();
        assertThat(query.filters())
            .contains(new IndicatorFilter(IndicatorCodes.SURVEY_YEAR_DIMENSION, FilterOperator.EQ, List.of(2023)));
    }

    @Test
    void toIndicatorQueryRejectsASeriesAggregationThePresetDoesNotAllow() {
        assertThatThrownBy(() -> catalog.toIndicatorQuery(
                catalog.get("oa-1"), List.of(), 2019, 2023, List.of(AggregationType.SUM)))
            .isInstanceOf(SeriesAggregationNotAllowedException.class)
            .hasMessageContaining("oa-1")
            .hasMessageContaining("SUM");
    }

    @Test
    void toIndicatorQueryAcceptsAnAllowedSeriesAggregation() {
        assertThatCode(() -> catalog.toIndicatorQuery(
                catalog.get("oa-1"), List.of(), 2019, 2023, List.of(AggregationType.AVG, AggregationType.MEDIAN)))
            .doesNotThrowAnyException();
    }
}
