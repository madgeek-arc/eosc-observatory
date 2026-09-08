package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Members of the "surveyYear" dimension: the calendar years of every <em>concluded</em> "eosc-sb"
 * survey. There is exactly one such survey per calendar year, the first in 2021, so the member list
 * is {@code 2021 .. (2020 + concludedCount)} inclusive — the first concluded survey is 2021, the
 * second 2022, and so on. Empty until at least one survey has concluded.
 *
 * <p>"Concluded" means the survey is no longer collecting answers:
 * {@code !model.isActive() && (model.getSubmissionCloseAt() == null
 * || model.getSubmissionCloseAt().before(new Date()))} — the same rule
 * {@code EmailSurveyService.checkSurveyDates} applies. {@code active} / {@code submissionCloseAt}
 * are not indexed on the {@code model} resourceType, so surveys are fetched in bulk and filtered in
 * memory (same as {@link SurveyIdDimensionMemberProvider}); no caching.
 *
 * <p>This list is <strong>advisory</strong>: it drives the {@code /dimensions/surveyYear/members}
 * dropdown only. {@code IndicatorQueryValidator} no longer rejects a {@code surveyYear} filter value
 * just because it is absent here — a query for a year the survey-stats-tool has no data for degrades
 * to {@code null} values and is logged (see {@code SurveyStatsToolIndicatorQueryHandler}). The
 * inherited {@link DimensionMemberProvider#validCodes} is therefore effectively unused for this
 * dimension but is left at the default so the contract still holds.
 *
 * <p>The {@code 2020 + concludedCount} formula assumes exactly one "eosc-sb" survey per calendar
 * year with none skipped; if that ever stops holding, derive each concluded survey's year from its
 * id ({@code m-eosc-sb-YYYY}) instead.
 */
@Service
public class SurveyYearDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "surveyYear";

    private static final String EOSC_SB_SURVEY_TYPE = "eosc-sb";
    /** The first "eosc-sb" survey ran in 2021; the Nth concluded survey is year (2020 + N). */
    private static final int FIRST_SURVEY_YEAR = 2021;
    private static final int FACET_FILTER_QUANTITY = 10_000;

    private final SurveyService surveyService;

    public SurveyYearDimensionMemberProvider(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @Override
    public String dimensionCode() {
        return DIMENSION_CODE;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        long concluded = fetchEoscSbSurveys().stream()
            .filter(SurveyYearDimensionMemberProvider::isConcluded)
            .count();
        int maxYear = FIRST_SURVEY_YEAR - 1 + (int) concluded; // == 2020 + concluded; < 2021 when concluded == 0

        List<DimensionMember> members = IntStream.rangeClosed(FIRST_SURVEY_YEAR, maxYear)
            .mapToObj(year -> new DimensionMember(Integer.toString(year), Integer.toString(year)))
            .toList(); // empty when maxYear < 2021

        return DimensionMemberSearch.filter(members, searchText, limit, false);
    }

    private List<Model> fetchEoscSbSurveys() {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(FACET_FILTER_QUANTITY);
        List<Model> results = surveyService.getByType(filter, EOSC_SB_SURVEY_TYPE).getResults();
        return results == null ? List.of() : results;
    }

    /** No longer collecting answers: inactive, and either no close date or a close date in the past. */
    private static boolean isConcluded(Model model) {
        return !model.isActive()
            && (model.getSubmissionCloseAt() == null || model.getSubmissionCloseAt().before(new Date()));
    }
}
