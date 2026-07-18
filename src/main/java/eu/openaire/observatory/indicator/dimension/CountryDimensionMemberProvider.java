package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.StakeholderService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Only "country" is a real dimension today. The member list is deliberately not EU-branded in
 * code: it's just every distinct Stakeholder.country value, which today happens to be EU member
 * states only because no non-EU stakeholder currently exists (see
 * INDICATOR_SEMANTIC_LAYER_PLAN-2nd_review_draft.md, §§15/23).
 */
@Service
public class CountryDimensionMemberProvider implements DimensionMemberProvider {

    private static final String COUNTRY_DIMENSION_CODE = "country";

    private final StakeholderService stakeholderService;

    public CountryDimensionMemberProvider(StakeholderService stakeholderService) {
        this.stakeholderService = stakeholderService;
    }

    @Override
    public String dimensionCode() {
        return COUNTRY_DIMENSION_CODE;
    }

    @Override
    public DimensionMemberPage search(String dimensionCode, String indicatorCode, String searchText, int limit) {
        if (!COUNTRY_DIMENSION_CODE.equals(dimensionCode)) {
            throw new UnknownDimensionException(dimensionCode);
        }

        String normalizedSearch = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);

        List<DimensionMember> members = stakeholderService.getAllCountryCodes().stream()
            .map(code -> new DimensionMember(code, new Locale("", code).getDisplayCountry(Locale.ENGLISH)))
            .filter(member -> normalizedSearch.isEmpty()
                || member.label().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                || member.code().toLowerCase(Locale.ROOT).contains(normalizedSearch))
            .sorted(Comparator.comparing(DimensionMember::label))
            .limit(limit)
            .toList();

        return new DimensionMemberPage(members);
    }
}
