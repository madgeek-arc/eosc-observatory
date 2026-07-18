package eu.openaire.observatory.indicator.dimension;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DimensionMemberProviderRegistryTest {

    private static DimensionMemberProvider providerFor(String code, DimensionMember... members) {
        return new DimensionMemberProvider() {
            @Override
            public String dimensionCode() {
                return code;
            }

            @Override
            public DimensionMemberPage search(String indicatorCode, String searchText, int limit) {
                return new DimensionMemberPage(List.of(members));
            }
        };
    }

    @Test
    void dispatchesToTheProviderRegisteredForTheDimensionCode() {
        var countryProvider = providerFor("country", new DimensionMember("GR", "Greece"));
        var initiativeTypeProvider = providerFor("initiativeType", new DimensionMember("X", "X"));
        var registry = new DimensionMemberProviderRegistry(List.of(countryProvider, initiativeTypeProvider));

        var page = registry.search("country", null, "", 100);

        assertThat(page.members()).containsExactly(new DimensionMember("GR", "Greece"));
    }

    @Test
    void throwsForUnregisteredDimensionCode() {
        var registry = new DimensionMemberProviderRegistry(List.of(providerFor("country")));

        assertThatThrownBy(() -> registry.search("industry", null, "", 100))
            .isInstanceOf(UnknownDimensionException.class);
    }
}
