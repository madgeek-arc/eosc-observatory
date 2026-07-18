package eu.openaire.observatory.indicator.mock;

import eu.openaire.observatory.indicator.validation.UnknownDimensionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockDimensionDefinitionLookupTest {

    private final MockDimensionDefinitionLookup lookup = new MockDimensionDefinitionLookup(new MockIndicatorCatalog());

    @Test
    void returnsTheRegisteredDimensionForAKnownCode() {
        var dimension = lookup.getRequired(MockIndicatorCatalog.COUNTRY_DIMENSION);

        assertThat(dimension.code()).isEqualTo(MockIndicatorCatalog.COUNTRY_DIMENSION);
    }

    @Test
    void throwsForAnUnregisteredCode() {
        assertThatThrownBy(() -> lookup.getRequired("unknown"))
            .isInstanceOf(UnknownDimensionException.class);
    }
}
