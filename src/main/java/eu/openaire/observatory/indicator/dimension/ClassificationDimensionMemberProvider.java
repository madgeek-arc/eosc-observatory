package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * Live-backed (via {@link OpenAireDimensionMemberFetcher}), unlike every other
 * DimensionMemberProvider in this package, all of which are static or local-DB-backed — this
 * dimension's values (document/output types like "Article" or "Dataset") only exist upstream.
 * classification's valid values genuinely differ by entity: datasets.count sees "Dataset"/
 * "Collection"/"Clinical Trial", publications.count sees "Article"/"Book"/"Conference object" — so
 * unlike the other providers, this one actually uses the indicatorCode parameter to pick the right
 * entity scope, caching each scope under its own key.
 */
@Service
public class ClassificationDimensionMemberProvider implements DimensionMemberProvider {

    private final OpenAireDimensionMemberFetcher fetcher;

    public ClassificationDimensionMemberProvider(OpenAireDimensionMemberFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String dimensionCode() {
        return IndicatorCodes.CLASSIFICATION_DIMENSION;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        String resultType = IndicatorCodes.DATASETS_COUNT.equals(indicatorCode)
            ? OpenAireQueryBuilder.RESULT_TYPE_DATASET : OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION;
        String cacheKey = "dimension-members:classification:" + resultType;

        DimensionMembers all = fetcher.fetchCached(cacheKey, IndicatorCodes.CLASSIFICATION_DIMENSION, resultType);
        return DimensionMemberSearch.filter(all.members(), searchText, limit, true);
    }
}
