package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Fixed, hardcoded member list for the "initiativeType" dimension used by the collapsed
 * oa.eu_country_coverage / oa.country_initiative_status mock indicators — there's no backing
 * storage for this vocabulary, unlike "country".
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
    public DimensionMemberPage search(String indicatorCode, String searchText, int limit) {
        String normalizedSearch = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);

        List<DimensionMember> members = MEMBERS.stream()
            .filter(member -> normalizedSearch.isEmpty()
                || member.label().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || member.code().toLowerCase(Locale.ROOT).contains(normalizedSearch))
            .limit(limit)
            .toList();

        return new DimensionMemberPage(members);
    }
}
