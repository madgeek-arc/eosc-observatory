package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.model.DimensionUsage;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.RenderHint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorDefinitionCatalogTest {

    private final IndicatorDefinitionCatalog catalog = new IndicatorDefinitionCatalog();

    @Test
    void containsAllEighteenRealDefinitions() {
        assertThat(catalog.getAll()).hasSize(18);
        assertThat(catalog.getAll()).extracting("code").containsExactlyInAnyOrder(
            IndicatorCodes.PUBLICATIONS_COUNT, IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, IndicatorCodes.DATASETS_COUNT,
            IndicatorCodes.SOFTWARE_COUNT, IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT, IndicatorCodes.DATASETS_ACCESS_STATUS_SHARE,
            IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.COUNTRY_INITIATIVE_STATUS, IndicatorCodes.EU_COUNTRY_COVERAGE,
            IndicatorCodes.COUNTRY_POLICY_STATUS, IndicatorCodes.MONITORED_AREAS_COUNT, IndicatorCodes.POLICY_AREAS_COUNT,
            IndicatorCodes.RESEARCHER_POLICY_COVERAGE,
            IndicatorCodes.LONG_TERM_PRESERVATION_COUNT, IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT,
            IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT,
            IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS, IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT
        );
    }

    @Test
    void financialInvestmentIsCountryAndInvestmentAreaFilterableAndGroupableWithOptionalSurveyYearFilter() {
        var bindings = catalog.get(IndicatorCodes.FINANCIAL_INVESTMENT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION, IndicatorCodes.INVESTMENT_AREA_DIMENSION
            );
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INVESTMENT_AREA_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INVESTMENT_AREA_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(catalog.get(IndicatorCodes.FINANCIAL_INVESTMENT).timePolicy().supported()).isTrue();
    }

    @Test
    void countryPolicyStatusRequiresInitiativeTypeAndSurveyYearFilters() {
        var bindings = catalog.get(IndicatorCodes.COUNTRY_POLICY_STATUS).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.INITIATIVE_TYPE_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION
            );
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INITIATIVE_TYPE_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(catalog.get(IndicatorCodes.COUNTRY_POLICY_STATUS).timePolicy().supported()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.MONITORED_AREAS_COUNT, IndicatorCodes.POLICY_AREAS_COUNT})
    void areaCountIndicatorsRequireOnlySurveyYearFilterAndAreOptionallyCountryFilterable(String indicatorCode) {
        var bindings = catalog.get(indicatorCode).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.SURVEY_YEAR_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(catalog.get(indicatorCode).renderHint()).isEqualTo(RenderHint.ENTITY_MAP);
    }

    @Test
    void softwareOpenSourceSetsCountIsDistinctFromSoftwareCount() {
        assertThat(catalog.get(IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT)).isNotNull();
        assertThat(catalog.get(IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT).executionBinding().handlerKey())
            .isEqualTo("survey-stats-tool");
        assertThat(catalog.get(IndicatorCodes.SOFTWARE_COUNT).executionBinding().handlerKey())
            .isEqualTo("openaire-stats-tool");
    }

    @Test
    void countryInitiativeStatusRequiresInitiativeTypeAndSurveyYearFilters() {
        var bindings = catalog.get(IndicatorCodes.COUNTRY_INITIATIVE_STATUS).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.INITIATIVE_TYPE_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION
            );
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INITIATIVE_TYPE_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
    }

    @Test
    void euCountryCoverageRequiresInitiativeTypeFilterWithOptionalSurveyYearFilter() {
        var definition = catalog.get(IndicatorCodes.EU_COUNTRY_COVERAGE);
        assertThat(definition.dimensionBindings()).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.INITIATIVE_TYPE_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION
            );
        assertThat(definition.dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INITIATIVE_TYPE_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(definition.dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.INITIATIVE_TYPE_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(definition.dimensionBindings())
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(definition.timePolicy().supported()).isTrue();
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
                IndicatorCodes.CLASSIFICATION_DIMENSION, IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION,
                IndicatorCodes.GOLD_OA_DIMENSION, IndicatorCodes.GREEN_OA_DIMENSION
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
    void publicationsCountGoldOaAndGreenOaAreFilterOnlyAndEqOnly() {
        var bindings = catalog.get(IndicatorCodes.PUBLICATIONS_COUNT).dimensionBindings();
        for (String dimensionCode : Set.of(IndicatorCodes.GOLD_OA_DIMENSION, IndicatorCodes.GREEN_OA_DIMENSION)) {
            assertThat(bindings)
                .filteredOn(binding -> binding.dimensionCode().equals(dimensionCode))
                .extracting("usages")
                .containsExactly(Set.of(DimensionUsage.FILTER));
            assertThat(bindings)
                .filteredOn(binding -> binding.dimensionCode().equals(dimensionCode))
                .extracting("allowedOperators")
                .containsExactly(Set.of(FilterOperator.EQ));
        }
    }

    @Test
    void oaLicenseStatusCountHasOptionalGroupableOaLicenseStatusAndIsCountryAndClassificationGroupable() {
        var bindings = catalog.get(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(
                IndicatorCodes.OA_LICENSE_STATUS_DIMENSION, IndicatorCodes.CLASSIFICATION_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION
            );
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.OA_LICENSE_STATUS_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.CLASSIFICATION_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.GROUP));
    }

    @Test
    void oaLicenseStatusCountRoutesToOpenAireHandler() {
        assertThat(catalog.get(IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT).executionBinding().handlerKey())
            .isEqualTo("openaire-stats-tool");
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

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {IndicatorCodes.DATASETS_COUNT, IndicatorCodes.SOFTWARE_COUNT})
    void entityCountIsCountryAndClassificationFilterableAndGroupableButNotFieldOfScience(String indicatorCode) {
        var definition = catalog.get(indicatorCode);
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
    void longTermPreservationCountIsCountryFilterableAndGroupableWithOptionalSurveyYearFilter() {
        var bindings = catalog.get(IndicatorCodes.LONG_TERM_PRESERVATION_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(catalog.get(IndicatorCodes.LONG_TERM_PRESERVATION_COUNT).timePolicy().supported()).isTrue();
    }

    @Test
    void citizenScienceNewProjectsCountIsCountryFilterableAndGroupableWithOptionalSurveyYearFilter() {
        var bindings = catalog.get(IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.COUNTRY_DIMENSION, IndicatorCodes.SURVEY_YEAR_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("usages")
            .containsExactly(Set.of(DimensionUsage.FILTER, DimensionUsage.GROUP));
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_YEAR_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
        assertThat(catalog.get(IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT).timePolicy().supported()).isTrue();
    }

    @Test
    void countriesWithValidatedAnswersRequiresSurveyIdFilterAndIsOptionallyCountryFilterable() {
        var bindings = catalog.get(IndicatorCodes.COUNTRIES_WITH_VALIDATED_ANSWERS).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode")
            .containsExactlyInAnyOrder(IndicatorCodes.SURVEY_ID_DIMENSION, IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.SURVEY_ID_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(true);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
    }

    @Test
    void stakeholderParticipatingCountriesCountIsOnlyOptionallyCountryFilterable() {
        var bindings = catalog.get(IndicatorCodes.STAKEHOLDER_PARTICIPATING_COUNTRIES_COUNT).dimensionBindings();
        assertThat(bindings).extracting("dimensionCode").containsExactly(IndicatorCodes.COUNTRY_DIMENSION);
        assertThat(bindings)
            .filteredOn(binding -> binding.dimensionCode().equals(IndicatorCodes.COUNTRY_DIMENSION))
            .extracting("requiredFilter")
            .containsExactly(false);
    }

    @Test
    void elevenDefinitionsSupportTimeSeries() {
        Set<String> timeSeriesCapable = Set.of(
            IndicatorCodes.PUBLICATIONS_COUNT, IndicatorCodes.PUBLICATIONS_ACCESS_STATUS_SHARE, IndicatorCodes.DATASETS_COUNT,
            IndicatorCodes.SOFTWARE_COUNT, IndicatorCodes.PUBLICATIONS_OA_LICENSE_STATUS_COUNT, IndicatorCodes.DATASETS_ACCESS_STATUS_SHARE,
            IndicatorCodes.FINANCIAL_INVESTMENT, IndicatorCodes.EU_COUNTRY_COVERAGE, IndicatorCodes.LONG_TERM_PRESERVATION_COUNT,
            IndicatorCodes.CITIZEN_SCIENCE_NEW_PROJECTS_COUNT, IndicatorCodes.SOFTWARE_OPEN_SOURCE_SETS_COUNT
        );

        assertThat(catalog.getAll())
            .filteredOn(definition -> timeSeriesCapable.contains(definition.code()))
            .hasSize(11)
            .allMatch(definition -> definition.timePolicy().supported());

        assertThat(catalog.getAll())
            .filteredOn(definition -> !timeSeriesCapable.contains(definition.code()))
            .hasSize(7)
            .noneMatch(definition -> definition.timePolicy().supported());
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
        assertThat(catalog.dimension("investmentArea")).isNotNull();
        assertThat(catalog.dimension("surveyId")).isNotNull();
        assertThat(catalog.dimension("surveyYear")).isNotNull();
        assertThat(catalog.dimension("classification")).isNotNull();
        assertThat(catalog.dimension("fieldOfScience")).isNotNull();
        assertThat(catalog.dimension("goldOA")).isNotNull();
        assertThat(catalog.dimension("greenOA")).isNotNull();
        assertThat(catalog.dimension("oaLicenseStatus")).isNotNull();
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
