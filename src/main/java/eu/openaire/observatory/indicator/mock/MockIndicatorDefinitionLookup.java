package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.validation.IndicatorDefinitionLookup;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import org.springframework.stereotype.Service;

@Service
public class MockIndicatorDefinitionLookup implements IndicatorDefinitionLookup {

    private final MockIndicatorCatalog catalog;

    public MockIndicatorDefinitionLookup(MockIndicatorCatalog catalog) {
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
