package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.model.RenderHint;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockIndicatorCatalogTest {

    private final MockIndicatorCatalog catalog = new MockIndicatorCatalog();

    @Test
    void containsTheFiveCollapsedDefinitions() {
        assertThat(catalog.getAll()).hasSize(5);
        assertThat(catalog.getAll()).extracting("code").containsExactlyInAnyOrder(
            MockIndicatorCatalog.PUBLICATIONS_COUNT, MockIndicatorCatalog.PUBLICATIONS_OA_SHARE,
            MockIndicatorCatalog.FINANCIAL_INVESTMENT, MockIndicatorCatalog.COUNTRY_INITIATIVE_STATUS,
            MockIndicatorCatalog.EU_COUNTRY_COVERAGE
        );
    }

    @Test
    void mapIndicatorUsesEntityMapRenderHint() {
        assertThat(catalog.get(MockIndicatorCatalog.COUNTRY_INITIATIVE_STATUS).renderHint()).isEqualTo(RenderHint.ENTITY_MAP);
    }

    @Test
    void euShareIndicatorsHaveNoCountryDimension() {
        assertThat(catalog.get(MockIndicatorCatalog.PUBLICATIONS_OA_SHARE).dimensionBindings()).isEmpty();
    }

    @Test
    void financialInvestmentIsCountryFilterableAndGroupable() {
        assertThat(catalog.get(MockIndicatorCatalog.FINANCIAL_INVESTMENT).dimensionBindings())
            .extracting("dimensionCode")
            .containsExactly("country");
    }

    @Test
    void euCountryCoverageAndCountryInitiativeStatusRequireInitiativeTypeFilter() {
        assertThat(catalog.get(MockIndicatorCatalog.EU_COUNTRY_COVERAGE).dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals("initiativeType"))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(catalog.get(MockIndicatorCatalog.COUNTRY_INITIATIVE_STATUS).dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals("initiativeType"))
            .extracting("requiredFilter")
            .containsExactly(true);
    }

    @Test
    void allDefinitionsSupportTimeSeries() {
        assertThat(catalog.getAll()).allMatch(definition -> definition.timePolicy().supported());
    }

    @Test
    void unknownCodeReturnsNull() {
        assertThat(catalog.get("999")).isNull();
    }

    @Test
    void dimensionReturnsRegisteredDimensions() {
        assertThat(catalog.dimension("country")).isNotNull();
        assertThat(catalog.dimension("initiativeType")).isNotNull();
        assertThat(catalog.dimension("accessStatus")).isNotNull();
        assertThat(catalog.dimension("unknown")).isNull();
    }

    @Test
    void everyDimensionBoundByEveryDefinitionIsRegistered() {
        for (var definition : catalog.getAll()) {
            for (var binding : definition.dimensionBindings()) {
                assertThat(catalog.dimension(binding.dimensionCode()))
                    .as("dimension %s bound by %s must be registered", binding.dimensionCode(), definition.code())
                    .isNotNull();
            }
        }
    }
}
