package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorDefinitionCatalog;
import eu.openaire.observatory.indicator.IndicatorQueryService;
import eu.openaire.observatory.indicator.IndicatorCatalog;
import eu.openaire.observatory.indicator.Indicator;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ActorContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "indicators/presets", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Indicators")
public class IndicatorPresetController {

    private final IndicatorCatalog presetCatalog;
    private final IndicatorDefinitionCatalog definitionCatalog;
    private final IndicatorQueryService queryService;
    private final ActorContext actorContext;

    public IndicatorPresetController(
            IndicatorCatalog presetCatalog, IndicatorDefinitionCatalog definitionCatalog,
            IndicatorQueryService queryService, ActorContext actorContext) {
        this.presetCatalog = presetCatalog;
        this.definitionCatalog = definitionCatalog;
        this.queryService = queryService;
        this.actorContext = actorContext;
    }

    @Operation(summary = "List all indicator presets")
    @GetMapping
    public ResponseEntity<IndicatorPresetResponse> getPresets() {
        var items = presetCatalog.getAll().stream().map(preset -> IndicatorPreset.from(preset, definitionCatalog)).toList();
        return ResponseEntity.ok(new IndicatorPresetResponse(items));
    }

    @Operation(summary = "Get a single indicator preset")
    @GetMapping("{id}")
    public ResponseEntity<IndicatorPreset> getPreset(@PathVariable String id) {
        Indicator preset = presetCatalog.get(id);
        if (preset == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(IndicatorPreset.from(preset, definitionCatalog));
    }

    /** {@code request.indicatorCode()} is ignored — the preset's own indicatorCode always wins. */
    @Operation(summary = "Run an indicator preset's query")
    @PostMapping("{id}/query")
    public ResponseEntity<IndicatorResult> queryPreset(@PathVariable String id, @RequestBody IndicatorQuery request) {
        Indicator preset = presetCatalog.get(id);
        if (preset == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(queryService.execute(preset.toIndicatorQuery(request), actorContext));
    }
}
