package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GreenOaDimensionMemberProviderTest {

    private final GreenOaDimensionMemberProvider provider = new GreenOaDimensionMemberProvider();

    @Test
    void returnsTheTwoFixedYesNoMembers() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder("YES", "NO");
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMembers page = provider.search(null, "no", 100);

        assertThat(page.members()).extracting("code").containsExactly("NO");
    }
}
