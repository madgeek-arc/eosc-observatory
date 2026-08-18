package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Live-backed (via {@link OpenAireDimensionMemberFetcher}), unlike every other
 * DimensionMemberProvider in this package, all of which are static or local-DB-backed — this
 * dimension's values (document/output types like "Article" or "Dataset") only exist upstream.
 * classification's valid values genuinely differ by entity: datasets.count sees "Dataset"/
 * "Collection"/"Clinical Trial", software.count sees "Software"/"Review"/"Other ORP type",
 * publications.count sees "Article"/"Book"/"Conference object" — so unlike the other providers,
 * this one actually uses the indicatorCode parameter to pick the right entity scope, caching each
 * scope under its own key. Any indicatorCode not in {@link #RESULT_TYPE_BY_INDICATOR} (including
 * null) defaults to the publication-scoped list.
 */
@Service
public class ClassificationDimensionMemberProvider implements DimensionMemberProvider {

    private static final Map<String, String> RESULT_TYPE_BY_INDICATOR = Map.of(
        IndicatorCodes.DATASETS_COUNT, OpenAireQueryBuilder.RESULT_TYPE_DATASET,
        IndicatorCodes.SOFTWARE_COUNT, OpenAireQueryBuilder.RESULT_TYPE_SOFTWARE
    );

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
        String resultType = indicatorCode == null
            ? OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION
            : RESULT_TYPE_BY_INDICATOR.getOrDefault(indicatorCode, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
        String cacheKey = "dimension-members:classification:" + resultType;

        DimensionMembers all = fetcher.fetchCached(cacheKey, IndicatorCodes.CLASSIFICATION_DIMENSION, resultType);
        return DimensionMemberSearch.filter(all.members(), searchText, limit, true);
    }
}
