package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorPreset;
import eu.openaire.observatory.indicator.IndicatorPresetCatalog;
import eu.openaire.observatory.indicator.IndicatorQueryService;
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

    private final IndicatorPresetCatalog presetCatalog;
    private final IndicatorQueryService queryService;
    private final ActorContext actorContext;

    public IndicatorPresetController(
            IndicatorPresetCatalog presetCatalog, IndicatorQueryService queryService, ActorContext actorContext) {
        this.presetCatalog = presetCatalog;
        this.queryService = queryService;
        this.actorContext = actorContext;
    }

    @Operation(summary = "List all indicator presets")
    @GetMapping
    public ResponseEntity<IndicatorPresetResponse> getPresets() {
        return ResponseEntity.ok(new IndicatorPresetResponse(presetCatalog.getAll()));
    }

    @Operation(summary = "Get a single indicator preset")
    @GetMapping("{id}")
    public ResponseEntity<IndicatorPreset> getPreset(@PathVariable String id) {
        IndicatorPreset preset = presetCatalog.get(id);
        if (preset == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(preset);
    }

    /** {@code request.indicatorCode()} is ignored — the preset's own indicatorCode always wins. */
    @Operation(summary = "Run an indicator preset's query")
    @PostMapping("{id}/query")
    public ResponseEntity<IndicatorResult> queryPreset(@PathVariable String id, @RequestBody IndicatorQuery request) {
        IndicatorPreset preset = presetCatalog.get(id);
        if (preset == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(queryService.execute(preset.toIndicatorQuery(request), actorContext));
    }
}
