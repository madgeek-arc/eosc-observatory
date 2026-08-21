package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitiativeTypeDimensionMemberProviderTest {

    private final InitiativeTypeDimensionMemberProvider provider = new InitiativeTypeDimensionMemberProvider();

    @Test
    void returnsTheTwentySevenFixedInitiativeTypes() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "OA_PUBLICATION_POLICY", "IMMEDIATE_OA_POLICY", "FINANCIAL_STRATEGY", "MONITORING_INITIATIVE",
            "OPEN_DATA_POLICY", "OPEN_DATA_FINANCIAL_STRATEGY", "OPEN_DATA_MONITORING_INITIATIVE",
            "FAIR_DATA_POLICY", "FAIR_DATA_FINANCIAL_STRATEGY", "FAIR_DATA_MONITORING_INITIATIVE",
            "DATA_MANAGEMENT_POLICY", "DATA_MANAGEMENT_FINANCIAL_STRATEGY", "DATA_MANAGEMENT_MONITORING_INITIATIVE",
            "CITIZEN_SCIENCE_POLICY", "CITIZEN_SCIENCE_FINANCIAL_STRATEGY", "CITIZEN_SCIENCE_MONITORING_INITIATIVE",
            "REPOSITORIES_CONNECT_POLICY", "REPOSITORIES_CONNECT_FINANCIAL_STRATEGY", "REPOSITORIES_CONNECT_MONITORING_INITIATIVE",
            "REPOSITORIES_PRESERVATION_POLICY", "REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE",
            "TRAINING_POLICY", "TRAINING_FINANCIAL_STRATEGY", "TRAINING_MONITORING_INITIATIVE",
            "SOFTWARE_POLICY", "SOFTWARE_FINANCIAL_STRATEGY", "SOFTWARE_MONITORING_INITIATIVE"
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMembers page = provider.search(null, "immediate", 100);

        assertThat(page.members()).extracting("code").containsExactly("IMMEDIATE_OA_POLICY");
    }

    @Test
    void filtersMatchAcrossTopicsForAScopedTerm() {
        DimensionMembers page = provider.search(null, "monitoring", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "MONITORING_INITIATIVE", "OPEN_DATA_MONITORING_INITIATIVE", "FAIR_DATA_MONITORING_INITIATIVE",
            "DATA_MANAGEMENT_MONITORING_INITIATIVE", "CITIZEN_SCIENCE_MONITORING_INITIATIVE",
            "REPOSITORIES_CONNECT_MONITORING_INITIATIVE", "REPOSITORIES_PRESERVATION_MONITORING_INITIATIVE",
            "TRAINING_MONITORING_INITIATIVE", "SOFTWARE_MONITORING_INITIATIVE"
        );
    }

    @Test
    void validCodesReturnsAllTwentySevenFixedInitiativeTypes() {
        assertThat(provider.validCodes(null)).hasSize(27).contains("OA_PUBLICATION_POLICY", "SOFTWARE_MONITORING_INITIATIVE");
    }
}
