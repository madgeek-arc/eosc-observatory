package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InitiativeTypeDimensionMemberProviderTest {

    private final InitiativeTypeDimensionMemberProvider provider = new InitiativeTypeDimensionMemberProvider();

    @Test
    void returnsTheFourFixedInitiativeTypes() {
        DimensionMemberPage page = provider.search("initiativeType", null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "OA_PUBLICATION_POLICY", "IMMEDIATE_OA_POLICY", "FINANCIAL_STRATEGY", "MONITORING_INITIATIVE"
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMemberPage page = provider.search("initiativeType", null, "monitoring", 100);

        assertThat(page.members()).extracting("code").containsExactly("MONITORING_INITIATIVE");
    }

    @Test
    void throwsForUnknownDimension() {
        assertThatThrownBy(() -> provider.search("country", null, "", 100))
            .isInstanceOf(UnknownDimensionException.class);
    }
}
