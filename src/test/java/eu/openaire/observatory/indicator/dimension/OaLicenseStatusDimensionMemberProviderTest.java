package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OaLicenseStatusDimensionMemberProviderTest {

    private final OaLicenseStatusDimensionMemberProvider provider = new OaLicenseStatusDimensionMemberProvider();

    @Test
    void returnsTheFiveFixedStatuses() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder(
            "OA_WITH_LICENSE", "OA_WITHOUT_LICENSE", "EMBARGOED", "RESTRICTED", "CLOSED"
        );
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMembers page = provider.search(null, "licence", 100);

        assertThat(page.members()).extracting("code")
            .containsExactlyInAnyOrder("OA_WITH_LICENSE", "OA_WITHOUT_LICENSE");
    }
}
