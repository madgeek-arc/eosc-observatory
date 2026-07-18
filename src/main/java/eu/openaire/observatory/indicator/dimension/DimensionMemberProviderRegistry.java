package eu.openaire.observatory.indicator.dimension;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DimensionMemberProviderRegistry {

    private final Map<String, DimensionMemberProvider> providersByDimensionCode;

    public DimensionMemberProviderRegistry(List<DimensionMemberProvider> providers) {
        this.providersByDimensionCode = providers.stream()
            .collect(Collectors.toUnmodifiableMap(
                DimensionMemberProvider::dimensionCode,
                Function.identity()
            ));
    }

    public DimensionMemberPage search(String dimensionCode, String indicatorCode, String searchText, int limit) {
        var provider = providersByDimensionCode.get(dimensionCode);
        if (provider == null) {
            throw new UnknownDimensionException(dimensionCode);
        }
        return provider.search(indicatorCode, searchText, limit);
    }
}
