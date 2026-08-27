package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.Indicator;

import java.util.List;
import java.util.Map;

/** API view of one {@link Indicator}, with {@code format}/{@code source} resolved from its indicator. */
public record IndicatorPreset(
    String id,
    String label,
    String group,
    String chartType,
    String format,
    String source,
    List<AggregationType> allowedSeriesAggregations,
    String indicatorCode,
    Map<String, List<String>> presetFilters
) {
    public static IndicatorPreset from(Indicator preset, IndicatorDefinitionCatalog catalog) {
        IndicatorDefinition indicator = catalog.get(preset.indicatorCode());
        if (indicator == null) {
            throw new IllegalStateException(
                "Indicator '" + preset.id() + "' references unknown indicatorCode: "
                    + preset.indicatorCode());
        }
        return new IndicatorPreset(
            preset.id(), preset.label(), preset.group().wireValue(),
            preset.chartType().name(), indicator.format().wireValue(), indicator.executionBinding().handlerKey(),
            preset.allowedSeriesAggregations(), preset.indicatorCode(), preset.presetFilters()
        );
    }
}
