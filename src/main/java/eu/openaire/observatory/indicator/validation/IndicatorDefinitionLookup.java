package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;

public interface IndicatorDefinitionLookup {

    /** @throws UnknownIndicatorException if no definition is registered under this code */
    IndicatorDefinition getRequired(String code);
}
