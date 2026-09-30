package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

/**
 * Fixed member list for the "investmentArea" dimension used by oa.financial_investment (survey
 * stats-tool-backed): one member per {@link OpenScienceArea}, each backed by its own numeric ("in
 * millions of Euros") survey question. There's no backing storage for this vocabulary.
 */
@Service
public class InvestmentAreaDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "investmentArea";

    private static final List<DimensionMember> MEMBERS = Stream.of(OpenScienceArea.values())
        .map(area -> new DimensionMember(area.name(), area.label()))
        .toList();

    @Override
    public String dimensionCode() {
        return DIMENSION_CODE;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        return DimensionMemberSearch.filter(MEMBERS, searchText, limit, false);
    }
}
