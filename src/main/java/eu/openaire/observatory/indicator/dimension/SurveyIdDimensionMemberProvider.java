package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.SurveyService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Members are the "eosc-sb" survey Models (e.g. "m-eosc-sb-2023") — fetched because
 * survey.countries_with_validated_answers' required surveyId filter is only meaningful against
 * a real survey id. Unrelated to stakeholder.participating_countries_count, which is hardcoded
 * to a same-named but distinct "eosc-sb" stakeholderType (see LocalRegistryIndicatorQueryHandler)
 * and has no surveyId binding at all.
 */
@Service
public class SurveyIdDimensionMemberProvider implements DimensionMemberProvider {

    private static final String SURVEY_ID_DIMENSION_CODE = "surveyId";
    private static final String EOSC_SB_SURVEY_TYPE = "eosc-sb";

    private final SurveyService surveyService;

    public SurveyIdDimensionMemberProvider(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @Override
    public String dimensionCode() {
        return SURVEY_ID_DIMENSION_CODE;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        FacetFilter filter = new FacetFilter();
        filter.setQuantity(10000);

        List<DimensionMember> members = surveyService.getByType(filter, EOSC_SB_SURVEY_TYPE).getResults().stream()
            .map(model -> new DimensionMember(model.getId(), model.getName()))
            .toList();

        return DimensionMemberSearch.filter(members, searchText, limit, true);
    }
}
