package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorDefinition;

import java.util.List;
import java.util.Map;

/**
 * UI-facing projection of one pre-defined dashboard widget — flat and self-contained, unlike
 * {@link IndicatorCatalogItem} (which exposes a generic indicator's full filter/group capability
 * set). {@code source} and {@code format} are derived from the backing indicator rather than
 * stored on the widget definition, so they can never drift out of sync: {@code source} from the
 * indicator's own execution binding ({@code openaire-stats-tool} / {@code survey-stats-tool} /
 * {@code local-registry}), {@code format} from the indicator's own {@code IndicatorFormat}.
 * {@code indicatorCode}/{@code presetFilters} are exposed so a client can build an
 * {@code IndicatorQuery} (and from it, a {@code DashboardItem}) for this widget itself, the same
 * way {@link eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorDefinition#toIndicatorQuery}
 * does server-side for the {@code /query} endpoint.
 */
public record PreDefinedIndicatorItem(
    String id,
    String label,
    String group,
    String chartType,
    String format,
    String source,
    boolean hasAverage,
    String indicatorCode,
    Map<String, List<String>> presetFilters
) {
    public static PreDefinedIndicatorItem from(PreDefinedIndicatorDefinition definition, IndicatorCatalog catalog) {
        IndicatorDefinition indicator = catalog.get(definition.indicatorCode());
        if (indicator == null) {
            throw new IllegalStateException(
                "PreDefinedIndicatorDefinition '" + definition.id() + "' references unknown indicatorCode: "
                    + definition.indicatorCode());
        }
        return new PreDefinedIndicatorItem(
            definition.id(), definition.label(), definition.group().wireValue(),
            definition.chartType().name(), indicator.format().wireValue(),
            indicator.executionBinding().handlerKey(), definition.hasAverage(),
            definition.indicatorCode(), definition.presetFilters()
        );
    }
}
