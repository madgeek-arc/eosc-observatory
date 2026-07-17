package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.dimension.DimensionMemberPage;
import eu.openaire.observatory.indicator.dimension.DimensionMemberProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "dimensions", produces = MediaType.APPLICATION_JSON_VALUE)
public class DimensionMemberController {

    private final DimensionMemberProvider dimensionMemberProvider;

    public DimensionMemberController(DimensionMemberProvider dimensionMemberProvider) {
        this.dimensionMemberProvider = dimensionMemberProvider;
    }

    @GetMapping("{code}/members")
    public ResponseEntity<DimensionMemberPage> getMembers(@PathVariable String code,
                                                            @RequestParam(defaultValue = "") String search,
                                                            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(dimensionMemberProvider.search(code, null, search, limit));
    }
}
