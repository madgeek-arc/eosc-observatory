package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorCatalogTest {

    private final IndicatorCatalog catalog = new IndicatorCatalog();

    @Test
    void containsTheTwoRealDefinitions() {
        assertThat(catalog.getAll()).hasSize(2);
        assertThat(catalog.getAll()).extracting("code").containsExactlyInAnyOrder(
            IndicatorCodes.PUBLICATIONS_COUNT, IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE
        );
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
        assertThat(catalog.dimension("accessStatus")).isNotNull();
        assertThat(catalog.dimension("initiativeType")).isNull();
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
