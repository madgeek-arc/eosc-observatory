package eu.openaire.observatory.indicator.validation;

import eu.openaire.observatory.indicator.model.DimensionDefinition;

public interface DimensionDefinitionLookup {

    /** Assumes the code has already been confirmed valid for the indicator being queried. */
    DimensionDefinition getRequired(String code);
}
