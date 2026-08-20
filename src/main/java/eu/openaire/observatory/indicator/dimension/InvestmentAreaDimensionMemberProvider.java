package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed, hardcoded member list for the "investmentArea" dimension used by oa.financial_investment
 * (survey stats-tool-backed) — there's no backing storage for this vocabulary. A separate
 * vocabulary from "initiativeType": the financial-investment survey questions are a distinct
 * numeric ("in millions of Euros") question series from the Yes/No policy/monitoring ones, so each
 * topic area needs its own question-code mapping — see
 * SurveyStatsToolIndicatorQueryHandler.QUESTION_CODE_BY_INVESTMENT_AREA.
 */
@Service
public class InvestmentAreaDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "investmentArea";

    private static final List<DimensionMember> MEMBERS = List.of(
        new DimensionMember("OA_PUBLICATION", "Financial Investment in OA Publication"),
        new DimensionMember("OPEN_DATA", "Financial Investment in Open Data"),
        new DimensionMember("FAIR_DATA", "Financial Investment in FAIR Data")
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
