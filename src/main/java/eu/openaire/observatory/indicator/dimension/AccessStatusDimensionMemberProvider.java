package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed, hardcoded member list for the "accessStatus" dimension used by publications.count and
 * publications.access_status_share — no backing storage for this vocabulary.
 * "OA" is the open side; "CLOSED", "EMBARGOED" and "RESTRICTED" are considered as non-open statuses.
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
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        return DimensionMemberSearch.filter(MEMBERS, searchText, limit, false);
    }
}
