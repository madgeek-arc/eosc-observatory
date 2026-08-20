package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.IndicatorQueryService;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorCatalog;
import eu.openaire.observatory.indicator.predefined.PreDefinedIndicatorDefinition;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ActorContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "indicators/pre-defined", produces = MediaType.APPLICATION_JSON_VALUE)
public class PreDefinedIndicatorController {

    private final PreDefinedIndicatorCatalog preDefinedCatalog;
    private final IndicatorCatalog catalog;
    private final IndicatorQueryService queryService;
    private final ActorContext actorContext;

    public PreDefinedIndicatorController(
            PreDefinedIndicatorCatalog preDefinedCatalog, IndicatorCatalog catalog,
            IndicatorQueryService queryService, ActorContext actorContext) {
        this.preDefinedCatalog = preDefinedCatalog;
        this.catalog = catalog;
        this.queryService = queryService;
        this.actorContext = actorContext;
    }

    @GetMapping
    public ResponseEntity<PreDefinedIndicatorResponse> getPreDefinedIndicators() {
        var items = preDefinedCatalog.getAll().stream().map(definition -> PreDefinedIndicatorItem.from(definition, catalog)).toList();
        return ResponseEntity.ok(new PreDefinedIndicatorResponse(items));
    }

    @GetMapping("{id}")
    public ResponseEntity<PreDefinedIndicatorItem> getPreDefinedIndicator(@PathVariable String id) {
        PreDefinedIndicatorDefinition definition = preDefinedCatalog.get(id);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(PreDefinedIndicatorItem.from(definition, catalog));
    }

    /** {@code request.indicatorCode()} is ignored — the widget's own indicatorCode always wins. */
    @PostMapping("{id}/query")
    public ResponseEntity<IndicatorResult> queryPreDefinedIndicator(@PathVariable String id, @RequestBody IndicatorQuery request) {
        PreDefinedIndicatorDefinition definition = preDefinedCatalog.get(id);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(queryService.execute(definition.toIndicatorQuery(request), actorContext));
    }
}
