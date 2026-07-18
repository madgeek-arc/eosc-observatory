package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;
import eu.openaire.observatory.indicator.validation.UnknownDimensionException;
import org.springframework.stereotype.Service;

@Service
public class MockDimensionDefinitionLookup implements DimensionDefinitionLookup {

    private final MockIndicatorCatalog catalog;

    public MockDimensionDefinitionLookup(MockIndicatorCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public DimensionDefinition getRequired(String code) {
        DimensionDefinition dimension = catalog.dimension(code);
        if (dimension == null) {
            throw new UnknownDimensionException(code);
        }
        return dimension;
    }
}
