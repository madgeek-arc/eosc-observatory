package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvestmentAreaDimensionMemberProviderTest {

    private final InvestmentAreaDimensionMemberProvider provider = new InvestmentAreaDimensionMemberProvider();

    @Test
    void returnsTheTwelveFixedInvestmentAreas() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "OA_PUBLICATION", "OPEN_DATA", "FAIR_DATA", "DATA_MANAGEMENT", "CITIZEN_SCIENCE",
            "SERVICES", "REPOSITORIES_CONNECT", "DATA_STEWARDSHIP", "REPOSITORIES_PRESERVATION",
            "TRAINING", "ASSESSMENT", "SOFTWARE"
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMembers page = provider.search(null, "citizen", 100);

        assertThat(page.members()).extracting("code").containsExactly("CITIZEN_SCIENCE");
    }

    @Test
    void dimensionCodeIsInvestmentArea() {
        assertThat(provider.dimensionCode()).isEqualTo("investmentArea");
    }
}
