package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed, hardcoded member list for the "greenOA" dimension used by publications.count — no backing
 * storage for this vocabulary, same shape as {@link AccessStatusDimensionMemberProvider}.
 */
@Service
public class GreenOaDimensionMemberProvider implements DimensionMemberProvider {

    private static final List<DimensionMember> MEMBERS = List.of(
        new DimensionMember("YES", "Yes"),
        new DimensionMember("NO", "No")
    );

    @Override
    public String dimensionCode() {
        return IndicatorCodes.GREEN_OA_DIMENSION;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        return DimensionMemberSearch.filter(MEMBERS, searchText, limit, false);
    }
}
