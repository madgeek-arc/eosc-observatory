package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitiativeTypeDimensionMemberProviderTest {

    private final InitiativeTypeDimensionMemberProvider provider = new InitiativeTypeDimensionMemberProvider();

    @Test
    void returnsEveryAreaByEveryInitiativeKindPlusImmediateOaPolicy() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).hasSize(37);
        for (OpenScienceArea area : OpenScienceArea.values()) {
            for (OpenScienceArea.InitiativeKind kind : OpenScienceArea.InitiativeKind.values()) {
                assertThat(page.members()).extracting("code").contains(area.initiativeType(kind));
            }
        }
        assertThat(page.members()).extracting("code").contains("IMMEDIATE_OA_POLICY", "OA_PUBLICATION_FINANCIAL_STRATEGY",
            "OA_PUBLICATION_MONITORING_INITIATIVE", "ASSESSMENT_POLICY", "REPOSITORIES_PRESERVATION_FINANCIAL_STRATEGY");
        assertThat(page.members()).extracting("code").doesNotContain("FINANCIAL_STRATEGY", "MONITORING_INITIATIVE");
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMembers page = provider.search(null, "immediate", 100);

        assertThat(page.members()).extracting("code").containsExactly("IMMEDIATE_OA_POLICY");
    }

    @Test
    void filtersMatchAcrossTopicsForAScopedTerm() {
        DimensionMembers page = provider.search(null, "monitoring", 100);

        assertThat(page.members()).extracting("code")
            .hasSize(12)
            .allMatch(code -> code.toString().endsWith("_MONITORING_INITIATIVE"));
    }

    @Test
    void validCodesReturnsAllThirtySevenFixedInitiativeTypes() {
        assertThat(provider.validCodes(null)).hasSize(37).contains("OA_PUBLICATION_POLICY", "SOFTWARE_MONITORING_INITIATIVE");
    }
}
