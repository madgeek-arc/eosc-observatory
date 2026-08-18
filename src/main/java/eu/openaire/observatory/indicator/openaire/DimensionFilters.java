package eu.openaire.observatory.indicator.openaire;

import java.util.List;

/**
 * The independently-optional, AND-composed filters {@link OpenAireQueryBuilder}'s result-count
 * sub-queries accept — one OR'd list of literals per dimension. Kept as a single parameter object
 * rather than growing the sub-query methods' positional-argument list further.
 */
public record DimensionFilters(
        List<String> countryCodes, List<String> classificationCodes, List<String> fieldOfScienceCodes,
        List<String> goldOaLiterals, List<String> greenOaLiterals) {

    public static final DimensionFilters NONE = new DimensionFilters(List.of(), List.of(), List.of(), List.of(), List.of());

    public DimensionFilters {
        countryCodes = countryCodes == null ? List.of() : List.copyOf(countryCodes);
        classificationCodes = classificationCodes == null ? List.of() : List.copyOf(classificationCodes);
        fieldOfScienceCodes = fieldOfScienceCodes == null ? List.of() : List.copyOf(fieldOfScienceCodes);
        goldOaLiterals = goldOaLiterals == null ? List.of() : List.copyOf(goldOaLiterals);
        greenOaLiterals = greenOaLiterals == null ? List.of() : List.copyOf(greenOaLiterals);
    }

    public static DimensionFilters ofCountry(List<String> countryCodes) {
        return new DimensionFilters(countryCodes, List.of(), List.of(), List.of(), List.of());
    }
}
