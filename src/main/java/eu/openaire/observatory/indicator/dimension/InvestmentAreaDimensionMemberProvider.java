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
        new DimensionMember("FAIR_DATA", "Financial Investment in FAIR Data"),
        new DimensionMember("DATA_MANAGEMENT", "Financial Investment in Data Management"),
        new DimensionMember("CITIZEN_SCIENCE", "Financial Investment in Citizen Science"),
        new DimensionMember("SERVICES", "Financial Investment in Offering Services through EOSC"),
        new DimensionMember("REPOSITORIES_CONNECT", "Financial Investment in Connecting Repositories to EOSC"),
        new DimensionMember("DATA_STEWARDSHIP", "Financial Investment in Data Stewardship"),
        new DimensionMember("REPOSITORIES_PRESERVATION", "Financial Investment in Long-Term Data Preservation"),
        new DimensionMember("TRAINING", "Financial Investment in Skills/Training for Open Science"),
        new DimensionMember("ASSESSMENT", "Financial Investment in Incentives/Rewards for Open Science"),
        new DimensionMember("SOFTWARE", "Financial Investment in Open-Source Software")
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
