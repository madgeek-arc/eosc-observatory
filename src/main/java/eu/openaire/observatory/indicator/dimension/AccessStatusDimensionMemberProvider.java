package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Fixed, hardcoded member list for the "accessStatus" dimension used by publications.count —
 * no backing storage for this vocabulary, unlike "country". "OA" is the open side; "CLOSED",
 * "EMBARGOED" and "RESTRICTED" are the three real non-open statuses (not a single collapsed
 * "Closed" bucket).
 */
@Service
public class AccessStatusDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "accessStatus";

    private static final List<DimensionMember> MEMBERS = List.of(
        new DimensionMember("OA", "Open Access"),
        new DimensionMember("CLOSED", "Closed Access"),
        new DimensionMember("EMBARGOED", "Embargoed"),
        new DimensionMember("RESTRICTED", "Restricted")
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
