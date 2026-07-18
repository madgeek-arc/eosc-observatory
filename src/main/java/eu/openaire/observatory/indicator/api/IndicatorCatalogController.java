package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.mock.MockIndicatorCatalog;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "indicators", produces = MediaType.APPLICATION_JSON_VALUE)
public class IndicatorCatalogController {

    private final MockIndicatorCatalog catalog;

    public IndicatorCatalogController(MockIndicatorCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public ResponseEntity<IndicatorCatalogResponse> getIndicators() {
        var items = catalog.getAll().stream().map(IndicatorCatalogItem::from).toList();
        return ResponseEntity.ok(new IndicatorCatalogResponse(items));
    }

    @GetMapping("{code}")
    public ResponseEntity<IndicatorCatalogItem> getIndicator(@PathVariable String code) {
        IndicatorDefinition definition = catalog.get(code);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(IndicatorCatalogItem.from(definition));
    }
}
