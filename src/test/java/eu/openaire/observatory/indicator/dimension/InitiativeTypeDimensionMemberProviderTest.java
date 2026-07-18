package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InitiativeTypeDimensionMemberProviderTest {

    private final InitiativeTypeDimensionMemberProvider provider = new InitiativeTypeDimensionMemberProvider();

    @Test
    void returnsTheFourFixedInitiativeTypes() {
        DimensionMemberPage page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "OA_PUBLICATION_POLICY", "IMMEDIATE_OA_POLICY", "FINANCIAL_STRATEGY", "MONITORING_INITIATIVE"
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMemberPage page = provider.search(null, "monitoring", 100);

        assertThat(page.members()).extracting("code").containsExactly("MONITORING_INITIATIVE");
    }
}
