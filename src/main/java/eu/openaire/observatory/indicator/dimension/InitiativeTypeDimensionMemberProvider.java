package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Fixed, hardcoded member list for the "initiativeType" dimension used by the collapsed
 * oa.eu_country_coverage / oa.country_initiative_status / oa.country_policy_status indicators (all
 * backed by the real survey stats tool) — there's no backing storage for this vocabulary. Despite
 * the dimension's "initiativeType" name and the OA_* members' historical naming, this is a
 * cross-topic vocabulary, not OA-Publication-only — see
 * SurveyStatsToolIndicatorQueryHandler.QUESTION_CODE_BY_INITIATIVE_TYPE for the real survey question
 * each member maps to, live-verified against the m-eosc-sb-2022/2023/2024/2025 registry models
 * (question numbering is stable across those years for every member here) plus a live raw?json= call
 * per code against both the 2022 and 2023 survey years. Every "*_POLICY" member also has a nested
 * "Question<N>.1" ("Is this policy mandatory?") sub-question, live-verified the same way — see
 * SurveyStatsToolIndicatorQueryHandler.mandatoryQuestionCode.
 */
@Service
public class InitiativeTypeDimensionMemberProvider implements DimensionMemberProvider {

    public static final String DIMENSION_CODE = "initiativeType";

    private static final List<DimensionMember> MEMBERS = List.of(
        new DimensionMember("OA_PUBLICATION_POLICY", "Policy on OA Publication"),
        new DimensionMember("IMMEDIATE_OA_POLICY", "Policy on Immediate OA Publications"),
        new DimensionMember("FINANCIAL_STRATEGY", "Financial Strategy on OA Publication"),
        new DimensionMember("MONITORING_INITIATIVE", "National OA Publication Monitoring Initiative"),
        new DimensionMember("OPEN_DATA_POLICY", "Policy on Open Data"),
        new DimensionMember("OPEN_DATA_FINANCIAL_STRATEGY", "Financial Strategy on Open Data"),
        new DimensionMember("OPEN_DATA_MONITORING_INITIATIVE", "National Open Data Monitoring Initiative"),
        new DimensionMember("FAIR_DATA_POLICY", "Policy on FAIR Data"),
        new DimensionMember("FAIR_DATA_FINANCIAL_STRATEGY", "Financial Strategy on FAIR Data"),
        new DimensionMember("FAIR_DATA_MONITORING_INITIATIVE", "National FAIR Data Monitoring Initiative"),
        new DimensionMember("DATA_MANAGEMENT_POLICY", "Policy on Data Management"),
        new DimensionMember("DATA_MANAGEMENT_FINANCIAL_STRATEGY", "Financial Strategy on Data Management"),
        new DimensionMember("DATA_MANAGEMENT_MONITORING_INITIATIVE", "National Data Management Monitoring Initiative"),
        new DimensionMember("CITIZEN_SCIENCE_POLICY", "Policy on Citizen Science"),
        new DimensionMember("CITIZEN_SCIENCE_FINANCIAL_STRATEGY", "Financial Strategy on Citizen Science"),
        new DimensionMember("CITIZEN_SCIENCE_MONITORING_INITIATIVE", "National Citizen Science Monitoring Initiative"),
        new DimensionMember("REPOSITORIES_CONNECT_POLICY", "Policy on Connecting Repositories to EOSC"),
        new DimensionMember("REPOSITORIES_CONNECT_FINANCIAL_STRATEGY", "Financial Strategy on Connecting Repositories to EOSC"),
        new DimensionMember("REPOSITORIES_CONNECT_MONITORING_INITIATIVE", "National Monitoring on Connecting Repositories to EOSC"),
        new DimensionMember("REPOSITORIES_PRESERVATION_POLICY", "Policy on Long-Term Data Preservation"),
        new DimensionMember("REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE", "National Monitoring on Long-Term Data Preservation"),
        new DimensionMember("TRAINING_POLICY", "Policy on Skills/Training for Open Science"),
        new DimensionMember("TRAINING_FINANCIAL_STRATEGY", "Financial Strategy on Skills/Training for Open Science"),
        new DimensionMember("TRAINING_MONITORING_INITIATIVE", "National Monitoring on Skills/Training for Open Science"),
        new DimensionMember("SOFTWARE_POLICY", "Policy on Open-Source Software"),
        new DimensionMember("SOFTWARE_FINANCIAL_STRATEGY", "Financial Strategy on Open-Source Software"),
        new DimensionMember("SOFTWARE_MONITORING_INITIATIVE", "National Monitoring on Open-Source Software")
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
