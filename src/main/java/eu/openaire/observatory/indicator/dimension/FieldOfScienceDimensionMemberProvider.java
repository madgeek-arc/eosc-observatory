package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * Live-backed (via {@link OpenAireDimensionMemberFetcher}) — publication-only, no dataset binding
 * exists for this dimension, so indicatorCode is never consulted.
 */
@Service
public class FieldOfScienceDimensionMemberProvider implements DimensionMemberProvider {

    private static final String CACHE_KEY = "dimension-members:fieldOfScience:" + OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION;

    private final OpenAireDimensionMemberFetcher fetcher;

    public FieldOfScienceDimensionMemberProvider(OpenAireDimensionMemberFetcher fetcher) {
        this.fetcher = fetcher;
    }

    @Override
    public String dimensionCode() {
        return IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION;
    }

    @Override
    public DimensionMembers search(String indicatorCode, String searchText, int limit) {
        DimensionMembers all = fetcher.fetchCached(
            CACHE_KEY, IndicatorCodes.FIELD_OF_SCIENCE_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
        return DimensionMemberSearch.filter(all.members(), searchText, limit, true);
    }
}
