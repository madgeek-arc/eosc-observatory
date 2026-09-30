package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed member list for the "initiativeType" dimension used by the collapsed
 * oa.eu_country_coverage / oa.country_initiative_status / oa.country_policy_status indicators (all
 * backed by the survey stats tool) — there's no backing storage for this vocabulary. It is the grid
 * of every {@link OpenScienceArea} by every {@link OpenScienceArea.InitiativeKind}, plus
 * {@link OpenScienceArea#IMMEDIATE_OA_POLICY}. Each member maps to one Yes/No survey question, and
 * every "*_POLICY" member also has a nested "Question<N>.1" ("Is this policy mandatory?")
 * sub-question.
 */
@Service
public class InitiativeTypeDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "initiativeType";

    private static final List<DimensionMember> MEMBERS = buildMembers();

    private static List<DimensionMember> buildMembers() {
        List<DimensionMember> members = new ArrayList<>();
        for (OpenScienceArea area : OpenScienceArea.values()) {
            for (OpenScienceArea.InitiativeKind kind : OpenScienceArea.InitiativeKind.values()) {
                members.add(new DimensionMember(area.initiativeType(kind), area.initiativeTypeLabel(kind)));
            }
            if (area == OpenScienceArea.OA_PUBLICATION) {
                members.add(new DimensionMember(OpenScienceArea.IMMEDIATE_OA_POLICY, "Policy on Immediate OA Publications"));
            }
        }
        return List.copyOf(members);
    }

    @Override
    public String dimensionCode() {
        return DIMENSION_CODE;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        return DimensionMemberSearch.filter(MEMBERS, searchText, limit, false);
    }
}
