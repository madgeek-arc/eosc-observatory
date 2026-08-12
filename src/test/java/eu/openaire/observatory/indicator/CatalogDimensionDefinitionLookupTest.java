package eu.openaire.observatory.indicator;

import eu.openaire.observatory.indicator.validation.UnknownDimensionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogDimensionDefinitionLookupTest {

    private final CatalogDimensionDefinitionLookup lookup = new CatalogDimensionDefinitionLookup(new IndicatorCatalog());

    @Test
    void returnsTheRegisteredDimensionForAKnownCode() {
        var dimension = lookup.getRequired(IndicatorCodes.COUNTRY_DIMENSION);

        assertThat(dimension.code()).isEqualTo(IndicatorCodes.COUNTRY_DIMENSION);
    }

    @Test
    void throwsForAnUnregisteredCode() {
        assertThatThrownBy(() -> lookup.getRequired("unknown"))
            .isInstanceOf(UnknownDimensionException.class);
    }
}
