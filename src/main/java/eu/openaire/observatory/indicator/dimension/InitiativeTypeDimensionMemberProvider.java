package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed, hardcoded member list for the "initiativeType" dimension used by the collapsed
 * oa.eu_country_coverage / oa.country_initiative_status indicators (both backed by the real
 * survey stats tool) — there's no backing storage for this vocabulary.
 */
@Service
public class InitiativeTypeDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "initiativeType";

    private static final List<DimensionMember> MEMBERS = List.of(
        new DimensionMember("OA_PUBLICATION_POLICY", "Policy on OA Publication"),
        new DimensionMember("IMMEDIATE_OA_POLICY", "Policy on Immediate OA Publications"),
        new DimensionMember("FINANCIAL_STRATEGY", "Financial Strategy on OA Publication"),
        new DimensionMember("MONITORING_INITIATIVE", "National OA Publication Monitoring Initiative")
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
