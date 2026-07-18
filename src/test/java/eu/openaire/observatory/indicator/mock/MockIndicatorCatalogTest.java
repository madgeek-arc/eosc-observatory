package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.model.RenderHint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockIndicatorCatalogTest {

    private final MockIndicatorCatalog catalog = new MockIndicatorCatalog();

    @Test
    void containsAllEightLegacyIndicators() {
        assertThat(catalog.getAll()).hasSize(8);
        assertThat(catalog.getAll()).extracting("code")
            .containsExactlyInAnyOrder("67", "68", "69", "70", "71", "72", "73", "74");
    }

    @Test
    void mapIndicatorsUseEntityMapRenderHint() {
        assertThat(catalog.get("73").renderHint()).isEqualTo(RenderHint.ENTITY_MAP);
        assertThat(catalog.get("74").renderHint()).isEqualTo(RenderHint.ENTITY_MAP);
    }

    @Test
    void europeWideRatioIndicatorsHaveNoCountryDimension() {
        assertThat(catalog.get("67").dimensions()).isEmpty();
        assertThat(catalog.get("68").dimensions()).isEmpty();
        assertThat(catalog.get("70").dimensions()).isEmpty();
        assertThat(catalog.get("71").dimensions()).isEmpty();
        assertThat(catalog.get("72").dimensions()).isEmpty();
    }

    @Test
    void financialInvestmentIsCountryFilterableAndGroupable() {
        assertThat(catalog.get("69").dimensions())
            .extracting("dimensionCode")
            .containsExactly("country");
    }

    @Test
    void unknownCodeReturnsNull() {
        assertThat(catalog.get("999")).isNull();
    }
}
