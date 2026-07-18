package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessStatusDimensionMemberProviderTest {

    private final AccessStatusDimensionMemberProvider provider = new AccessStatusDimensionMemberProvider();

    @Test
    void returnsTheFourFixedAccessStatuses() {
        DimensionMemberPage page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code")
            .containsExactlyInAnyOrder("OA", "CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMemberPage page = provider.search(null, "closed", 100);

        assertThat(page.members()).extracting("code").containsExactly("CLOSED");
    }
}
