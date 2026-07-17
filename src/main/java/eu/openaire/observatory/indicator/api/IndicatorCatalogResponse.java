package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;

import java.util.List;

public record IndicatorCatalogResponse(List<IndicatorDefinition> items) {
}
