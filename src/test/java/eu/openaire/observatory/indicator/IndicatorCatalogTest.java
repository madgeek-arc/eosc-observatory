package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.RenderHint;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorCatalogTest {

    private final IndicatorCatalog catalog = new IndicatorCatalog();

    @Test
    void containsAllElevenRealDefinitions() {
        assertThat(catalog.getAll()).hasSize(11);
        assertThat(catalog.getAll()).extracting("code").containsExactlyInAnyOrder(
            IndicatorCodes.PUBLICATIONS_COUNT, IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, IndicatorCodes.DATASETS_COUNT,
            IndicatorCodes.SOFTWARE_COUNT,
            IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.EU_COUNTRY_COVERAGE,
            IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT,
            IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS, IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT
        );
    }

    @Test
    void financialInvestmentIsCountryFilterableAndGroupable() {
        var bindings = catalog.get(IndicatorCodes.FINANCIAL_INVESTMENT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
    }

    @Test
    void countryInitiativeStatusRequiresInitiativeTypeFilter() {
        var bindings = catalog.get(IndicatorCodes.COUNTRY_INITIATIVE_STATUS).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode").containsExactly(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INITIATIVE_TYPE_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
    }

    @Test
    void euCountryCoverageRequiresInitiativeTypeFilterAndUsesEntityMapRenderHint() {
        var definition = catalog.get(IndicatorCodes.EU_COUNTRY_COVERAGE);
        assertThat(definition.dimensionBindings()).extracting("dimensionCode")
            .containsExactly(IndicatorCodes.INITIATIVE_TYPE_DIMENSION);
        assertThat(definition.dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INITIATIVE_TYPE_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
    }

    @Test
    void countryInitiativeStatusUsesEntityMapRenderHint() {
        assertThat(catalog.get(IndicatorCodes.COUNTRY_INITIATIVE_STATUS).renderHint())
            .isEqualTo(RenderHint.ENTITY_MAP);
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
            .containsExactlyInAnyOrder(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION,
                IndicatorCodes.CLASSIFICATION_DIMENSION, IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION
            );
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
    void publicationsCountClassificationAndFieldOfScienceAreFilterableAndGroupable() {
        var bindings = catalog.get(IndicatorCodes.PUBLICATIONS_COUNT).dimensionBindings();
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.CLASSIFICATION_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
    }

    @Test
    void datasetsCountIsCountryAndClassificationFilterableAndGroupableButNotFieldOfScience() {
        var definition = catalog.get(IndicatorCodes.DATASETS_COUNT);
        assertThat(definition.executionBinding().handlerKey()).isEqualTo("openaire-stats-tool");

        var bindings = definition.dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.CLASSIFICATION_DIMENSION
            );
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.CLASSIFICATION_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
    }

    @Test
    void softwareCountIsCountryAndClassificationFilterableAndGroupableButNotFieldOfScience() {
        var definition = catalog.get(IndicatorCodes.SOFTWARE_COUNT);
        assertThat(definition.executionBinding().handlerKey()).isEqualTo("openaire-stats-tool");

        var bindings = definition.dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.ACCESS_STATUS_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.CLASSIFICATION_DIMENSION
            );
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.CLASSIFICATION_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
    }

    @Test
    void longTermPreservationCountIsCountryFilterableAndGroupable() {
        var bindings = catalog.get(IndicatorCodes.LONG_TERM_PRESERVATION_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
    }

    @Test
    void citizenScienceNewProjectsCountIsCountryFilterableAndGroupable() {
        var bindings = catalog.get(IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
    }

    @Test
    void countriesWithValidatedAnswersRequiresSurveyIdFilter() {
        var bindings = catalog.get(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode").containsExactly(IndicatorCodes.SURVEY_ID_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_ID_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
    }

    @Test
    void stakeholderParticipatingCountriesCountHasNoDimensionBindings() {
        assertThat(catalog.get(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT).dimensionBindings()).isEmpty();
    }

    @Test
    void nineDefinitionsSupportTimeSeriesButTheTwoLocalRegistryOnesDoNot() {
        assertThat(catalog.getAll())
            .filteredOn(definition -> !definition.code().equals(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS)
                && !definition.code().equals(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT))
            .allMatch(definition -> definition.timePolicy().supported());

        assertThat(catalog.get(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS).timePolicy().supported()).isFalse();
        assertThat(catalog.get(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT).timePolicy().supported()).isFalse();
    }

    @Test
    void unknownCodeReturnsNull() {
        assertThat(catalog.get("999")).isNull();
    }

    @Test
    void dimensionReturnsRegisteredDimensions() {
        assertThat(catalog.dimension("country")).isNotNull();
        assertThat(catalog.dimension("accessStatus")).isNotNull();
        assertThat(catalog.dimension("initiativeType")).isNotNull();
        assertThat(catalog.dimension("surveyId")).isNotNull();
        assertThat(catalog.dimension("classification")).isNotNull();
        assertThat(catalog.dimension("fieldOfScience")).isNotNull();
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
