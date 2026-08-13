package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.service.StakeholderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Country dimension members are derived from the distinct {@code Stakeholder.country} values
 * in the database. The field is free text with no EU or vocabulary restriction, so whatever
 * countries stakeholders are registered under will appear here.
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
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        List<DimensionMember> members = stakeholderService.getAllCountryCodes().stream()
            .map(code -> new DimensionMember(code, new Locale("", code).getDisplayCountry(Locale.ENGLISH)))
            .toList();

        return DimensionMemberSearch.filter(members, searchText, limit, true);
    }
}
