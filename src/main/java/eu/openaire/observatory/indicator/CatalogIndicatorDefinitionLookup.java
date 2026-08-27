package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.validation.IndicatorDefinitionLookup;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import org.springframework.stereotype.Service;

@Service
public class CatalogIndicatorDefinitionLookup implements IndicatorDefinitionLookup {

    private final IndicatorDefinitionCatalog catalog;

    public CatalogIndicatorDefinitionLookup(IndicatorDefinitionCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public IndicatorDefinition getRequired(String code) {
        IndicatorDefinition definition = catalog.get(code);
        if (definition == null) {
            throw new UnknownIndicatorException(code);
        }
        return definition;
    }
}
