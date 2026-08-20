package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorDefinition;

/**
 * UI-facing projection of one pre-defined dashboard widget — flat and self-contained, unlike
 * {@link IndicatorCatalogItem} (which exposes a generic indicator's full filter/group capability
 * set). {@code source} is derived from the backing indicator's own execution binding rather than
 * stored on the widget definition, so it can never drift out of sync with which handler actually
 * answers the query: the three handler keys in use today are already the exact wire values this
 * needs ({@code openaire-stats-tool} / {@code survey-stats-tool} / {@code local-registry}).
 */
public record PreDefinedIndicatorItem(
    String id,
    String label,
    String group,
    String kind,
    String format,
    String source,
    boolean hasAverage
) {
    public static PreDefinedIndicatorItem from(PreDefinedIndicatorDefinition definition, IndicatorCatalog catalog) {
        IndicatorDefinition indicator = catalog.get(definition.indicatorCode());
        if (indicator == null) {
            throw new IllegalStateException(
                "PreDefinedIndicatorDefinition '" + definition.id() + "' references unknown indicatorCode: "
                    + definition.indicatorCode());
        }
        return new PreDefinedIndicatorItem(
            definition.id(), definition.label(), definition.group(),
            definition.kind().wireValue(), definition.format().wireValue(),
            indicator.executionBinding().handlerKey(), definition.hasAverage()
        );
    }
}
