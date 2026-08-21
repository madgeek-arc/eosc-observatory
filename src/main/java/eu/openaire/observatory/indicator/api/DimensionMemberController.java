package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.dimension.DimensionMembers;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProviderRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nested under the indicator (rather than a flat {@code /dimensions/{code}/members}) because a
 * dimension's valid members can genuinely differ per indicator — e.g. "classification" members are
 * publication document types for publications.count but dataset document types for datasets.count.
 * {@link DimensionMemberProvider#search} already accepted an indicatorCode for exactly this; this
 * controller is what finally supplies a real one instead of always passing null.
 */
@RestController
@RequestMapping(path = "indicators", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Indicators")
public class DimensionMemberController {

    private final DimensionMemberProviderRegistry dimensionMemberProviderRegistry;

    public DimensionMemberController(DimensionMemberProviderRegistry dimensionMemberProviderRegistry) {
        this.dimensionMemberProviderRegistry = dimensionMemberProviderRegistry;
    }

    @Operation(summary = "Search valid dimension member values for an indicator")
    @GetMapping("{indicatorCode}/dimensions/{code}/members")
    public ResponseEntity<DimensionMembers> getMembers(@PathVariable String indicatorCode,
                                                       @PathVariable String code,
                                                       @RequestParam(defaultValue = "") String search,
                                                       @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(dimensionMemberProviderRegistry.search(code, indicatorCode, search, limit));
    }
}
