package eu.openaire.observatory.indicator.openaire;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps the catalog's YES/NO member codes (used by the "goldOA"/"greenOA" dimensions) to OpenAIRE
 * stats-tool's own literal "1"/"0" values for the result.gold_oa/result.green_oa fields (confirmed
 * live against the real endpoint).
 */
public enum OpenAireBooleanFlag {

    YES("1"),
    NO("0");

    private static final Map<String, OpenAireBooleanFlag> BY_CATALOG_CODE = Stream.of(values())
        .collect(Collectors.toMap(Enum::name, flag -> flag));

    private final String openAireLiteral;

    OpenAireBooleanFlag(String openAireLiteral) {
        this.openAireLiteral = openAireLiteral;
    }

    public String openAireLiteral() {
        return openAireLiteral;
    }

    public static OpenAireBooleanFlag fromCatalogCode(String catalogCode) {
        OpenAireBooleanFlag flag = BY_CATALOG_CODE.get(catalogCode);
        if (flag == null) {
            throw new IllegalArgumentException("Unknown boolean-flag code: " + catalogCode);
        }
        return flag;
    }
}
