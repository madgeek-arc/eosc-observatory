package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.DimensionDefinition;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;
import eu.openaire.observatory.indicator.validation.UnknownDimensionException;
import org.springframework.stereotype.Service;

@Service
public class CatalogDimensionDefinitionLookup implements DimensionDefinitionLookup {

    private final IndicatorCatalog catalog;

    public CatalogDimensionDefinitionLookup(IndicatorCatalog catalog) {
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
