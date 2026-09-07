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
import eu.openaire.observatory.indicator.validation.InvalidPresetScopeException;
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
    void containsEightyTwoPresets() {
        assertThat(catalog.getAll()).hasSize(82);
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
        assertThat(all.getLast().id()).isEqualTo("osp-6");
    }

    @Test
    void distributionOfOaTypesPresetsExistAndAreFullWidth() {
        IndicatorPreset oaDistribution = catalog.get("oa-9");
        IndicatorPreset odDistribution = catalog.get("od-8");

        assertThat(oaDistribution).isNotNull();
        assertThat(oaDistribution.indicatorCode()).isEqualTo(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT);
        assertThat(oaDistribution.fullWidth()).isTrue();

        assertThat(odDistribution).isNotNull();
        assertThat(odDistribution.indicatorCode()).isEqualTo(IndicatorCodes.DATASETS_COUNT);
        assertThat(odDistribution.fullWidth()).isTrue();
    }

    @Test
    void distributionPresetsCarryAnAccessTypeSelectorOnEveryView() {
        for (String id : List.of("oa-9", "od-8")) {
            IndicatorPreset preset = catalog.get(id);
            assertThat(preset.views()).as(id + " views").allSatisfy(view -> {
                assertThat(view.selector()).as(id + " " + view.renderStyle() + " selector").isNotNull();
                assertThat(view.selector().label()).isEqualTo("Access type");
            });
        }
        assertThat(catalog.get("oa-9").views().get(0).selector().dimension())
            .isEqualTo(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION);
        assertThat(catalog.get("od-8").views().get(0).selector().dimension())
            .isEqualTo(IndicatorCodes.ACCESS_STATUS_DIMENSION);
    }

    @Test
    void everyOtherPresetIsNotFullWidth() {
        Set<String> fullWidthIds = Set.of(
            "oa-9", "od-8", "ost-1", "ost-2", "nm-1", "nm-4", "osp-1", "osp-2", "osp-6");
        for (IndicatorPreset preset : catalog.getAll()) {
            assertThat(preset.fullWidth())
                .as(preset.id() + " fullWidth")
                .isEqualTo(fullWidthIds.contains(preset.id()));
        }
    }

    @Test
    void toIndicatorQueryBuildsACountryFilterAndGroupsByCountryForSelectedCountries() {
        IndicatorQuery query = catalog.toIndicatorQuery(
            catalog.get("oa-1"), List.of("EL", "IT"), 2019, 2023, List.of());

        assertThat(query.filters())
            .contains(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("EL", "IT")));
        assertThat(query.groupBy()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void toIndicatorQueryDoesNotGroupByCountryForAnAllCountriesScalarView() {
        // oa-1 ALL_COUNTRIES / SINGLE_YEAR view is SCALAR
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("oa-1"), List.of(), 2023, 2023, List.of());

        assertThat(query.groupBy()).isEmpty();
    }

    @Test
    void toIndicatorQueryDoesNotGroupByCountryForAnAllCountriesLineChartView() {
        // oa-1 ALL_COUNTRIES / TIME_RANGE view is LINE_CHART
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("oa-1"), List.of(), 2019, 2023, List.of());

        assertThat(query.groupBy()).isEmpty();
    }

    @Test
    void toIndicatorQueryGroupsByCountryForAnAllCountriesChoroplethView() {
        // cs-1 ALL_COUNTRIES / SINGLE_YEAR view is CHOROPLETH_MAP_WITH_TOP_5
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("cs-1"), List.of(), 2023, 2023, List.of());

        assertThat(query.groupBy()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(query.filters()).noneMatch(f -> f.dimension().equals(IndicatorCodes.COUNTRY_DIMENSION));
    }

    @Test
    void toIndicatorQueryGroupsByCountryForAnAllCountriesMapView() {
        // oa-7 (NATIONAL_STATUS_VIEWS) ALL_COUNTRIES / SINGLE_YEAR view is MAP
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("oa-7"), List.of(), 2023, 2023, List.of());

        assertThat(query.groupBy()).containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
    }

    // ---- distribution presets: the selector dimension is always grouped, so one series comes back
    //      per member and the UI shows the one the actor picked (oa-9 = publications.oa_license_status_count,
    //      whose classification breakdown the handler always applies). ------------------------------

    @Test
    void distributionPresetGroupsByTheSelectorDimensionAndClassificationForTheEuStackedColumnCell() {
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("oa-9"), List.of(), 2023, 2023, List.of());

        assertThat(query.groupBy())
            .containsExactly(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, IndicatorCodes.CLASSIFICATION_DIMENSION);
        assertThat(query.filters()).isEmpty();
        assertThat(query.timeSeries()).isEqualTo(new TimeSeriesRequest(2023, 2023, null));
    }

    @Test
    void distributionPresetGroupsByTheSelectorDimensionOnlyForTheEuTrendCell() {
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("oa-9"), List.of(), 2019, 2023, List.of());

        assertThat(query.groupBy()).containsExactly(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION);
        assertThat(query.timeSeries()).isEqualTo(new TimeSeriesRequest(2019, 2023, null));
    }

    @Test
    void distributionPresetAddsCountryForTheSelectedCountriesCells() {
        IndicatorQuery singleYear = catalog.toIndicatorQuery(catalog.get("oa-9"), List.of("EL", "IT"), 2023, 2023, List.of());
        assertThat(singleYear.groupBy())
            .containsExactly(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(singleYear.filters())
            .contains(new IndicatorFilter(IndicatorCodes.COUNTRY_DIMENSION, FilterOperator.IN, List.of("EL", "IT")));

        IndicatorQuery range = catalog.toIndicatorQuery(catalog.get("oa-9"), List.of("EL", "IT"), 2019, 2023, List.of());
        assertThat(range.groupBy())
            .containsExactly(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void openDataDistributionPresetGroupsByAccessStatusWithoutForcingClassificationOffTheStackedColumn() {
        // od-8 backs on datasets.count — classification IS bound there, so the stacked column keeps it
        IndicatorQuery stacked = catalog.toIndicatorQuery(catalog.get("od-8"), List.of(), 2023, 2023, List.of());
        assertThat(stacked.groupBy())
            .containsExactly(IndicatorCodes.ACCESS_STATUS_DIMENSION, IndicatorCodes.CLASSIFICATION_DIMENSION);

        IndicatorQuery trend = catalog.toIndicatorQuery(catalog.get("od-8"), List.of(), 2019, 2023, List.of());
        assertThat(trend.groupBy()).containsExactly(IndicatorCodes.ACCESS_STATUS_DIMENSION);
    }

    @Test
    void investmentByAreaDistributionPresetGroupsByInvestmentAreaWithNoClassification() {
        // fin-2 backs on oa.financial_investment, which does not bind classification at all
        IndicatorQuery stacked = catalog.toIndicatorQuery(catalog.get("fin-2"), List.of(), 2023, 2023, List.of());
        assertThat(stacked.groupBy()).containsExactly(IndicatorCodes.INVESTMENT_AREA_DIMENSION);
    }

    @Test
    void nonDistributionPresetDerivesGroupByExactlyAsBefore() {
        // oa-1 has no selector on any view — the legacy perCountryBreakdown() path
        assertThat(catalog.toIndicatorQuery(catalog.get("oa-1"), List.of(), 2023, 2023, List.of()).groupBy()).isEmpty();
        assertThat(catalog.toIndicatorQuery(catalog.get("oa-1"), List.of("EL"), 2023, 2023, List.of()).groupBy())
            .containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
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
            catalog.get("oa-8"), List.of(), 2019, 2023, List.of());

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

    @Test
    void toIndicatorQueryRejectsAMissingYearTo() {
        assertThatThrownBy(() -> catalog.toIndicatorQuery(catalog.get("oa-1"), List.of(), 2019, null, List.of()))
            .isInstanceOf(InvalidPresetScopeException.class)
            .hasMessageContaining("yearTo");
    }

    @Test
    void toIndicatorQueryRejectsAMissingYearFromForATrendCapablePreset() {
        assertThatThrownBy(() -> catalog.toIndicatorQuery(catalog.get("oa-1"), List.of(), null, 2023, List.of()))
            .isInstanceOf(InvalidPresetScopeException.class)
            .hasMessageContaining("yearFrom");
    }

    @Test
    void toIndicatorQueryRejectsYearFromAfterYearTo() {
        assertThatThrownBy(() -> catalog.toIndicatorQuery(catalog.get("oa-1"), List.of(), 2024, 2023, List.of()))
            .isInstanceOf(InvalidPresetScopeException.class)
            .hasMessageContaining("yearFrom");
    }

    // ---- cross-cutting pages: Open Science Trends / National Monitoring / Open Science Policies ----

    @Test
    void theThreeCrossCuttingPagesEachHaveTheirInventoryPresets() {
        assertThat(idsInGroup(IndicatorGroup.OPEN_SCIENCE_TRENDS)).containsExactly("ost-1", "ost-2", "ost-3", "ost-4");
        assertThat(idsInGroup(IndicatorGroup.NATIONAL_MONITORING)).containsExactly("nm-1", "nm-2", "nm-3", "nm-4");
        assertThat(idsInGroup(IndicatorGroup.OPEN_SCIENCE_POLICIES))
            .containsExactly("osp-1", "osp-2", "osp-3", "osp-4", "osp-5", "osp-6");
    }

    @Test
    void byAreaPresetsPinEveryTopicAreaAndDeriveAnInitiativeTypeGroupBy() {
        // nm-1 ("… by Areas"): EU-wide single year -> group by initiativeType only, one series per area.
        IndicatorQuery euSingle = catalog.toIndicatorQuery(catalog.get("nm-1"), List.of(), 2023, 2023, List.of());
        assertThat(euSingle.groupBy()).containsExactly(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(euSingle.filters()).anySatisfy(f -> {
            assertThat(f.dimension()).isEqualTo(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
            assertThat(f.operator()).isEqualTo(FilterOperator.IN);
            assertThat(f.values()).hasSize(9);
        });

        // selected countries -> initiativeType + country
        IndicatorQuery selected = catalog.toIndicatorQuery(catalog.get("nm-1"), List.of("EL", "IT"), 2023, 2023, List.of());
        assertThat(selected.groupBy())
            .containsExactly(IndicatorCodes.INITIATIVE_TYPE_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void byCountriesAndAreasMatrixPresetGroupsByInitiativeType() {
        IndicatorQuery query = catalog.toIndicatorQuery(catalog.get("osp-6"), List.of(), 2023, 2023, List.of());
        assertThat(query.groupBy()).contains(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(query.filters()).anySatisfy(f ->
            assertThat(f.dimension()).isEqualTo(IndicatorCodes.INITIATIVE_TYPE_DIMENSION));
    }

    @Test
    void researcherCoveragePresetIsRegisteredEvenThoughItsHandlerIsStubbed() {
        IndicatorPreset preset = catalog.get("osp-5");
        assertThat(preset).isNotNull();
        assertThat(preset.indicatorCode()).isEqualTo(IndicatorCodes.RESEARCHER_POLICY_COVERAGE);
        assertThat(definitionCatalog.get(preset.indicatorCode())).isNotNull();
    }

    private List<String> idsInGroup(IndicatorGroup group) {
        return catalog.getAll().stream()
            .filter(preset -> preset.group() == group)
            .map(IndicatorPreset::id)
            .toList();
    }
}
