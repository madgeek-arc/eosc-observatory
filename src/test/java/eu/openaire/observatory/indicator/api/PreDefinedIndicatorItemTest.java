package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.local.LocalRegistryIndicatorQueryHandler;
import eu.openaire.observatory.indicator.openaire.OpenAireStatsToolIndicatorQueryHandler;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorDefinition;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorFormat;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorKind;
import eu.openaire.observatory.indicator.survey.SurveyStatsToolIndicatorQueryHandler;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PreDefinedIndicatorItemTest {

    private final IndicatorCatalog catalog = new IndicatorCatalog();

    @Test
    void derivesSourceFromTheOpenAireHandler() {
        var definition = widget(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, Map.of());

        var item = PreDefinedIndicatorItem.from(definition, catalog);

        assertThat(item.source()).isEqualTo(OpenAireStatsToolIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void derivesSourceFromTheSurveyHandler() {
        var definition = widget(IndicatorCodes.FINANCIAL_INVESTMENT, Map.of());

        var item = PreDefinedIndicatorItem.from(definition, catalog);

        assertThat(item.source()).isEqualTo(SurveyStatsToolIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void derivesSourceFromTheLocalRegistryHandler() {
        var definition = widget(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT, Map.of());

        var item = PreDefinedIndicatorItem.from(definition, catalog);

        assertThat(item.source()).isEqualTo(LocalRegistryIndicatorQueryHandler.HANDLER_KEY);
    }

    @Test
    void serializesKindAndFormatToTheirWireValues() {
        var definition = new PreDefinedIndicatorDefinition(
            "od-4", "National Policy on Open Data", "Open Data",
            PreDefinedIndicatorKind.MAP, PreDefinedIndicatorFormat.YES_NO, false,
            IndicatorCodes.COUNTRY_INITIATIVE_STATUS, Map.of()
        );

        var item = PreDefinedIndicatorItem.from(definition, catalog);

        assertThat(item.kind()).isEqualTo("map");
        assertThat(item.format()).isEqualTo("yes-no");
        assertThat(item.id()).isEqualTo("od-4");
        assertThat(item.label()).isEqualTo("National Policy on Open Data");
        assertThat(item.group()).isEqualTo("Open Data");
    }

    @Test
    void throwsWhenTheReferencedIndicatorCodeDoesNotExist() {
        var definition = widget("does.not.exist", Map.of());

        assertThatThrownBy(() -> PreDefinedIndicatorItem.from(definition, catalog))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("does.not.exist");
    }

    private static PreDefinedIndicatorDefinition widget(String indicatorCode, Map<String, java.util.List<String>> presetFilters) {
        return new PreDefinedIndicatorDefinition(
            "widget-id", "Widget Label", "Widget Group",
            PreDefinedIndicatorKind.INDICATOR, PreDefinedIndicatorFormat.NUMBER, false,
            indicatorCode, presetFilters
        );
    }
}
