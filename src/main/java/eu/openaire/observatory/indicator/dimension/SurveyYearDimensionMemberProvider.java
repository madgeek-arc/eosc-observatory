package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.survey.SurveyEditions;
import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Members of the "surveyYear" dimension: the reference years (the years the data describes) of
 * every <em>concluded</em> "eosc-sb" survey listed in {@link SurveyEditions}, in ascending order.
 * Empty until at least one such survey has concluded.
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
 */
@Service
public class SurveyYearDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "surveyYear";

    private static final String EOSC_SB_SURVEY_TYPE = "eosc-sb";
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
        List<DimensionMember> members = fetchEoscSbSurveys().stream()
            .filter(SurveyYearDimensionMemberProvider::isConcluded)
            .flatMap(model -> SurveyEditions.forSurveyId(model.getId()).stream())
            .map(SurveyEditions.Edition::referenceYear)
            .sorted()
            .map(year -> new DimensionMember(Integer.toString(year), Integer.toString(year)))
            .toList();

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
