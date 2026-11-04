package eu.openaire.observatory.indicator.survey;

import java.util.List;
import java.util.Optional;

/**
 * The "eosc-sb" surveys served by indicators, keyed by reference year: the year a survey's data
 * describes, which is the year indicator queries ask for. A survey's id and its stats-tool year
 * can't be derived from that year, so they are listed here. Older surveys have unrelated ids, and
 * the first survey ({@code s-xcTgCfWO}) is left out because its questions have a different format.
 *
 * <p>Temporary: a new survey needs a new entry here until this data lives on the survey model.
 */
public final class SurveyEditions {

    /** One survey: the year its data describes, its model id, and its {@code year} in the stats-tool. */
    public record Edition(int referenceYear, String surveyId, int statsYear) {
    }

    private static final List<Edition> EDITIONS = List.of(
        new Edition(2021, "m-jlFggsCN", 2022),
        new Edition(2022, "m-eosc-sb-2023", 2023),
        new Edition(2023, "m-eosc-sb-2024", 2024),
        new Edition(2024, "m-eosc-sb-2025", 2025)
    );

    private SurveyEditions() {
    }

    public static Optional<Edition> forReferenceYear(int referenceYear) {
        return EDITIONS.stream().filter(edition -> edition.referenceYear() == referenceYear).findFirst();
    }

    public static Optional<Edition> forSurveyId(String surveyId) {
        return EDITIONS.stream().filter(edition -> edition.surveyId().equals(surveyId)).findFirst();
    }
}
