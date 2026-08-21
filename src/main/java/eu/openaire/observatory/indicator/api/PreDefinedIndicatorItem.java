package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorDefinition;

import java.util.List;
import java.util.Map;

/**
 * UI-facing projection of one pre-defined dashboard widget — flat and self-contained, unlike
 * {@link IndicatorCatalogItem} (which exposes a generic indicator's full filter/group capability
 * set). {@code source}, {@code format}, {@code supportsTrend}, and {@code supportsMap} are derived
 * from the backing indicator rather than stored on the widget definition, so they can never drift
 * out of sync: {@code source} from the indicator's own execution binding
 * ({@code openaire-stats-tool} / {@code survey-stats-tool} / {@code local-registry}),
 * {@code format} from the indicator's own {@code IndicatorFormat}, {@code supportsTrend} from
 * {@code IndicatorDefinition.timePolicy().supported()} (the same fact
 * {@link IndicatorCatalogItem#supportsTimeRange()} exposes for the generic catalog), and
 * {@code supportsMap} from whether the indicator's {@code RenderHint} is {@code ENTITY_MAP}. These
 * are capability flags only — which chart type to actually render (and save on a
 * {@code DashboardItem}) is entirely a UI decision, not something computed here.
 * {@code allowedSeriesAggregations} is a straight pass-through from the widget definition (not
 * derived from the indicator) — the exact {@code AggregationType} values a client should put in
 * {@code IndicatorQuery.seriesAggregations} for this widget, same type as that field so it can be
 * copied across directly.
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
    List<AggregationType> allowedSeriesAggregations,
    boolean supportsTrend,
    boolean supportsMap,
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
            definition.chartType().name(), indicator.format().wireValue(), indicator.executionBinding().handlerKey(),
            definition.allowedSeriesAggregations(), indicator.timePolicy().supported(), indicator.renderHint() == RenderHint.ENTITY_MAP,
            definition.indicatorCode(), definition.presetFilters()
        );
    }
}
