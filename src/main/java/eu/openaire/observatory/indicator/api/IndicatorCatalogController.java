package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "indicators/definitions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Indicators")
public class IndicatorCatalogController {

    private final IndicatorDefinitionCatalog catalog;
    private final DimensionDefinitionLookup dimensionLookup;

    public IndicatorCatalogController(IndicatorDefinitionCatalog catalog, DimensionDefinitionLookup dimensionLookup) {
        this.catalog = catalog;
        this.dimensionLookup = dimensionLookup;
    }

    @Operation(summary = "List all indicator catalog definitions")
    @GetMapping
    public ResponseEntity<IndicatorCatalogResponse> getIndicators() {
        var items = catalog.getAll().stream().map(definition -> IndicatorCatalogItem.from(definition, dimensionLookup)).toList();
        return ResponseEntity.ok(new IndicatorCatalogResponse(items));
    }

    @Operation(summary = "Get a single indicator definition by code")
    @GetMapping("{code}")
    public ResponseEntity<IndicatorCatalogItem> getIndicator(@PathVariable String code) {
        IndicatorDefinition definition = catalog.get(code);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(IndicatorCatalogItem.from(definition, dimensionLookup));
    }
}
