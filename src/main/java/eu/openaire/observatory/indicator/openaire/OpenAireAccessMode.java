package eu.openaire.observatory.indicator.openaire;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps the catalog's "accessStatus" dimension member codes to OpenAIRE stats-tool's
 * own literal "result.access mode" values (confirmed against observatory-ui's explore-queries.ts,
 * which is the only place these literals are otherwise recorded).
 */
public enum OpenAireAccessMode {

    OA("Open Access"),
    CLOSED("Closed Access"),
    EMBARGOED("Embargo"),
    RESTRICTED("Restricted");

    private static final Map<String, OpenAireAccessMode> BY_CATALOG_CODE = Stream.of(values())
        .collect(Collectors.toMap(Enum::name, mode -> mode));

    private final String openAireLiteral;

    OpenAireAccessMode(String openAireLiteral) {
        this.openAireLiteral = openAireLiteral;
    }

    public String openAireLiteral() {
        return openAireLiteral;
    }

    public static OpenAireAccessMode fromCatalogCode(String catalogCode) {
        OpenAireAccessMode mode = BY_CATALOG_CODE.get(catalogCode);
        if (mode == null) {
            throw new IllegalArgumentException("Unknown accessStatus code: " + catalogCode);
        }
        return mode;
    }
}
