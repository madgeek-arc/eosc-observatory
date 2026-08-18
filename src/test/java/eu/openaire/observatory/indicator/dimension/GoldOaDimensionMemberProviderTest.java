package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoldOaDimensionMemberProviderTest {

    private final GoldOaDimensionMemberProvider provider = new GoldOaDimensionMemberProvider();

    @Test
    void returnsTheTwoFixedYesNoMembers() {
        DimensionMembers page = provider.search(null, "", 100);

        assertThat(page.members()).extracting("code").containsExactlyInAnyOrder("YES", "NO");
    }

    @Test
    void filtersByCaseInsensitiveSearchTextOnLabelOrCode() {
        DimensionMembers page = provider.search(null, "yes", 100);

        assertThat(page.members()).extracting("code").containsExactly("YES");
    }
}
