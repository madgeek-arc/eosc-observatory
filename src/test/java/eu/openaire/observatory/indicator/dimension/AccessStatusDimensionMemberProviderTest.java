package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessStatusDimensionMemberProviderTest {

    private final AccessStatusDimensionMemberProvider provider = new AccessStatusDimensionMemberProvider();

    @Test
    void returnsTheFourFixedAccessStatuses() {
        DimensionMemberPage page = provider.search("accessStatus", null, "", 100);

        assertThat(page.members()).extracting("code")
            .containsExactlyInAnyOrder("OA", "CLOSED", "EMBARGOED", "RESTRICTED");
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMemberPage page = provider.search("accessStatus", null, "closed", 100);

        assertThat(page.members()).extracting("code").containsExactly("CLOSED");
    }

    @Test
    void throwsForUnknownDimension() {
        assertThatThrownBy(() -> provider.search("country", null, "", 100))
            .isInstanceOf(UnknownDimensionException.class);
    }
}
