package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.KeyedRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DimensionMemberProviderRegistry {

    private final KeyedRegistry<String, DimensionMemberProvider> providersByDimensionCode;

    public DimensionMemberProviderRegistry(List<DimensionMemberProvider> providers) {
        this.providersByDimensionCode = new KeyedRegistry<>(providers, DimensionMemberProvider::dimensionCode);
    }

    public DimensionMembers search(String dimensionCode, String indicatorCode, String searchText, int limit) {
        DimensionMemberProvider provider = providersByDimensionCode.getRequired(dimensionCode, UnknownDimensionException::new);
        return provider.search(indicatorCode, searchText, limit);
    }

    /** Empty for an unknown dimension code. */
    public Optional<DimensionMemberProvider> find(String dimensionCode) {
        return providersByDimensionCode.find(dimensionCode);
    }
}
