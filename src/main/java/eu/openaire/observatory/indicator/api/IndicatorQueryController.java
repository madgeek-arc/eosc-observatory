package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorQueryService;
import eu.openaire.observatory.indicator.query.IndicatorQuery;
import eu.openaire.observatory.indicator.result.IndicatorResult;
import eu.openaire.observatory.indicator.validation.ActorContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "indicator-queries", produces = MediaType.APPLICATION_JSON_VALUE)
public class IndicatorQueryController {

    private final IndicatorQueryService queryService;
    private final ActorContext actorContext;

    public IndicatorQueryController(IndicatorQueryService queryService, ActorContext actorContext) {
        this.queryService = queryService;
        this.actorContext = actorContext;
    }

    @PostMapping
    public ResponseEntity<IndicatorResult> query(@RequestBody IndicatorQuery query) {
        return ResponseEntity.ok(queryService.execute(query, actorContext));
    }
}
