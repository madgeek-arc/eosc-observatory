package eu.openaire.observatory.indicator.api;

import eu.openaire.observatory.indicator.IndicatorFixtures;
import eu.openaire.observatory.indicator.model.AggregationPolicy;
import eu.openaire.observatory.indicator.model.AggregationType;
import eu.openaire.observatory.indicator.model.DimensionType;
import eu.openaire.observatory.indicator.model.FilterOperator;
import eu.openaire.observatory.indicator.model.IndicatorAccessLevel;
import eu.openaire.observatory.indicator.model.IndicatorDefinition;
import eu.openaire.observatory.indicator.model.IndicatorExecutionBinding;
import eu.openaire.observatory.indicator.model.IndicatorSemanticType;
import eu.openaire.observatory.indicator.model.IndicatorStatus;
import eu.openaire.observatory.indicator.model.IndicatorValueType;
import eu.openaire.observatory.indicator.model.MissingPeriodHandling;
import eu.openaire.observatory.indicator.model.NullHandling;
import eu.openaire.observatory.indicator.model.RenderHint;
import eu.openaire.observatory.indicator.model.TemporalBehavior;
import eu.openaire.observatory.indicator.model.TimeGrain;
import eu.openaire.observatory.indicator.model.TimePolicy;
import eu.openaire.observatory.indicator.model.UnitType;
import eu.openaire.observatory.indicator.validation.DimensionDefinitionLookup;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IndicatorCatalogItemTest {

    private final DimensionDefinitionLookup dimensionLookup = code -> {
        if (IndicatorFixtures.COUNTRY_DIMENSION.equals(code)) {
            return IndicatorFixtures.countryDimension();
        }
        throw new IllegalStateException("unexpected dimension in test: " + code);
    };

    @Test
    void exposesEachDimensionBindingAsACapability() {
        var definition = IndicatorFixtures.countIndicator("publications.count");

        var item = IndicatorCatalogItem.from(definition, dimensionLookup);

        assertThat(item.id()).isEqualTo("publications.count");
        assertThat(item.format()).isEqualTo("number");
        assertThat(item.supportsTimeRange()).isTrue();
        assertThat(item.dimensions()).hasSize(1);

        IndicatorDimensionCapability capability = item.dimensions().get(0);
        assertThat(capability.dimensionCode()).isEqualTo(IndicatorFixtures.COUNTRY_DIMENSION);
        assertThat(capability.label()).isEqualTo("Country");
        assertThat(capability.type()).isEqualTo(DimensionType.ENTITY_REFERENCE);
        assertThat(capability.filterable()).isTrue();
        assertThat(capability.groupable()).isTrue();
        assertThat(capability.requiredFilter()).isFalse();
        assertThat(capability.allowedOperators()).containsExactlyInAnyOrder(FilterOperator.EQ, FilterOperator.IN);
    }

    @Test
    void definitionWithNoDimensionsProducesEmptyCapabilityList() {
        var definition = IndicatorFixtures.ratioIndicator("publications.acceptance_rate");

        var item = IndicatorCatalogItem.from(definition, dimensionLookup);

        assertThat(item.format()).isEqualTo("percentage");
        assertThat(item.dimensions()).isEmpty();
    }

    @Test
    void mapRenderHintProducesMapFormat() {
        var definition = mapIndicator("oa.country_initiative_status");

        var item = IndicatorCatalogItem.from(definition, dimensionLookup);

        assertThat(item.format()).isEqualTo("map");
    }

    private static IndicatorDefinition mapIndicator(String code) {
        return new IndicatorDefinition(
            UUID.randomUUID(), code, "National OA Initiative Status", "desc", "Publications",
            IndicatorValueType.BOOLEAN, IndicatorSemanticType.ATTRIBUTE, UnitType.NONE, RenderHint.ENTITY_MAP,
            new AggregationPolicy(AggregationType.NONE, Set.of(AggregationType.NONE), NullHandling.PRESERVE),
            null,
            Set.of(),
            new TimePolicy(
                true, "year", Set.of(TimeGrain.YEAR), TimeGrain.YEAR, TemporalBehavior.PERIOD_VALUE, MissingPeriodHandling.ZERO_FILL
            ),
            new IndicatorExecutionBinding("mock", code),
            IndicatorAccessLevel.PUBLIC, IndicatorStatus.ACTIVE, 1
        );
    }
}
