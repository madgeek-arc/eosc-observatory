package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.local.LocalRegistryIndicatorQueryHandler;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.ChartType;
import eu.openaire.observatory.indicator.openaire.OpenAireStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.Indicator;
import eu.openaire.observatory.indicator.model.IndicatorGroup;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndicatorPresetTest {

    private final IndicatorDefinitionCatalog catalog = new IndicatorDefinitionCatalog();

    @Test
    void derivesSourceFromTheOpenAireHandler() {
        var definition = preset(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, Map.of());

        var item = IndicatorPreset.from(definition, catalog);

        assertThat(item.source()).isEqualTo(OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void derivesSourceFromTheSurveyHandler() {
        var definition = preset(IndicatorCodes.FINANCIAL_INVESTMENT, Map.of());

        var item = IndicatorPreset.from(definition, catalog);

        assertThat(item.source()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void derivesSourceFromTheLocalRegistryHandler() {
        var definition = preset(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT, Map.of());

        var item = IndicatorPreset.from(definition, catalog);

        assertThat(item.source()).isEqualTo(LocalRegistryIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void serializesKindAndFormatToTheirWireValues() {
        var definition = new Indicator(
            "od-4", "National Policy on Open Data", IndicatorGroup.OPEN_DATA,
            ChartType.MAP, List.of(),
            IndicatorCodes.COUNTRY_INITIATIVE_STATUS, Map.of()
        );

        var item = IndicatorPreset.from(definition, catalog);

        assertThat(item.chartType()).isEqualTo("MAP");
        assertThat(item.format()).isEqualTo("yes-no");
        assertThat(item.id()).isEqualTo("od-4");
        assertThat(item.label()).isEqualTo("National Policy on Open Data");
        assertThat(item.group()).isEqualTo("Open Data");
        assertThat(item.allowedSeriesAggregations()).isEmpty();
    }

    @Test
    void scalarPresetExposesItsAllowedSeriesAggregations() {
        var definition = preset(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, Map.of());

        var item = IndicatorPreset.from(definition, catalog);

        assertThat(item.chartType()).isEqualTo("SCALAR");
        assertThat(item.allowedSeriesAggregations()).containsExactly(AggregationType.AVG, AggregationType.MEDIAN);
    }

    @Test
    void throwsWhenTheReferencedIndicatorCodeDoesNotExist() {
        var definition = preset("does.not.exist", Map.of());

        assertThatThrownBy(() -> IndicatorPreset.from(definition, catalog))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does.not.exist");
    }

    private static Indicator preset(String indicatorCode, Map<String, java.util.List<String>> presetFilters) {
        return new Indicator(
            "preset-id", "Preset Label", IndicatorGroup.OPEN_DATA,
            ChartType.SCALAR, List.of(AggregationType.AVG, AggregationType.MEDIAN),
            indicatorCode, presetFilters
        );
    }
}
