package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitiativeTypeDimensionMemberProviderTest {

    private final InitiativeTypeDimensionMemberProvider provider = new InitiativeTypeDimensionMemberProvider();

    @Test
    void returnsTheTenFixedInitiativeTypes() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "OA_PUBLICATION_POLICY", "IMMEDIATE_OA_POLICY", "FINANCIAL_STRATEGY", "MONITORING_INITIATIVE",
            "OPEN_DATA_POLICY", "OPEN_DATA_FINANCIAL_STRATEGY", "OPEN_DATA_MONITORING_INITIATIVE",
            "FAIR_DATA_POLICY", "FAIR_DATA_FINANCIAL_STRATEGY", "FAIR_DATA_MONITORING_INITIATIVE"
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
            "MONITORING_INITIATIVE", "OPEN_DATA_MONITORING_INITIATIVE", "FAIR_DATA_MONITORING_INITIATIVE"
        );
    }
}
