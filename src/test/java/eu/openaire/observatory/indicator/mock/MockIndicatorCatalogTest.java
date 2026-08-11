package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.RenderHint;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MockIndicatorCatalogTest {

    private final MockIndicatorCatalog catalog = new MockIndicatorCatalog();

    @Test
    void containsTheFiveCollapsedDefinitions() {
        assertThat(catalog.getAll()).hasSize(5);
        assertThat(catalog.getAll()).extracting("code").containsExactlyInAnyOrder(
            IndicatorCodes.PUBLICATIONS_COUNT, IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE,
            IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.COUNTRY_INITIATIVE_STATUS,
            IndicatorCodes.EU_COUNTRY_COVERAGE
        );
    }

    @Test
    void mapIndicatorUsesEntityMapRenderHint() {
        assertThat(catalog.get(IndicatorCodes.COUNTRY_INITIATIVE_STATUS).renderHint()).isEqualTo(RenderHint.ENTITY_MAP);
    }

    @Test
    void accessStatusShareRequiresAccessStatusFilterAndIsCountryFilterableAndGroupable() {
        var bindings = catalog.get(IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.ACCESS_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.ACCESS_STATUS_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.ACCESS_STATUS_DIMENSION))
            .extracting("allowedOperators")
            .containsExactly(Set.of(FilterOperator.EQ, FilterOperator.IN, FilterOperator.NOT_EQ, FilterOperator.NOT_IN));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("allowedOperators")
            .containsExactly(Set.of(FilterOperator.EQ, FilterOperator.IN));
    }

    @Test
    void publicationsCountIsCountryFilterableAndGroupable() {
        var bindings = catalog.get(IndicatorCodes.PUBLICATIONS_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.ACCESS_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("allowedOperators")
            .containsExactly(Set.of(FilterOperator.EQ, FilterOperator.IN));
    }

    @Test
    void financialInvestmentIsCountryFilterableAndGroupable() {
        assertThat(catalog.get(IndicatorCodes.FINANCIAL_INVESTMENT).dimensionBindings())
            .extracting("dimensionCode")
            .containsExactly("country");
    }

    @Test
    void euCountryCoverageAndCountryInitiativeStatusRequireInitiativeTypeFilter() {
        assertThat(catalog.get(IndicatorCodes.EU_COUNTRY_COVERAGE).dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals("initiativeType"))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(catalog.get(IndicatorCodes.COUNTRY_INITIATIVE_STATUS).dimensionBindings())
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
