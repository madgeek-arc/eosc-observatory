package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed, hardcoded member list for the "surveyYear" dimension, required by all 5
 * survey-stats-tool-backed indicators. Limited to the years confirmed live for every one of
 * those 5 indicators' underlying questions — repositories.long_term_preservation_count and
 * citizen_science.new_projects_count are also confirmed for 2024, but the 3 initiativeType-driven
 * indicators are not, so this list stays at the safe common intersection rather than varying by
 * indicatorCode.
 */
@Service
public class SurveyYearDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "surveyYear";

    private static final List<DimensionMember> MEMBERS = List.of(
        new DimensionMember("2022", "2022"),
        new DimensionMember("2023", "2023")
    );

    @Override
    public String dimensionCode() {
        return DIMENSION_CODE;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        return DimensionMemberSearch.filter(MEMBERS, searchText, limit, false);
    }
}
