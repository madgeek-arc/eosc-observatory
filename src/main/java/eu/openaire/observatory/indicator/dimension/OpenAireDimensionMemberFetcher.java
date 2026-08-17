package eu.openaire.observatory.indicator.dimension;

import eu.openaire.observatory.indicator.openaire.DimensionFilters;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import eu.openaire.observatory.indicator.openaire.OpenAireStatsToolClient;
import eu.openaire.observatory.service.CacheService;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Year;
import java.util.List;

/**
 * Shared by classification/fieldOfScience's DimensionMemberProviders: fetches the distinct values
 * of an OpenAIRE breakdown field by issuing the same groupBy sub-query
 * OpenAireStatsToolIndicatorQueryHandler uses for the equivalent query's GROUP usage, then caches
 * the result — the stats-tool times out (confirmed live, 90s) if the mandatory year filter is
 * omitted to ask for "every year", so this queries one fixed recent year as a proxy for the full
 * vocabulary, same tradeoff as SurveyStatsToolIndicatorQueryHandler.DEFAULT_YEAR. Cached with a
 * daily TTL (unlike this codebase's only other CacheService usage,
 * SurveyAnswerCrudService/SurveyServiceImpl, which never expires) — these vocabularies change
 * rarely, but daily refresh means a newly-added classification/field-of-science value shows up
 * without needing an app restart or a manual cache flush.
 */
@Component
public class OpenAireDimensionMemberFetcher {

    private static final Duration CACHE_TTL = Duration.ofDays(1);

    /** Most recent complete year — this year's data is still accumulating, so its member set would be an undercount. */
    private static final int REFERENCE_YEAR = Year.now().getValue() - 1;

    private final OpenAireQueryBuilder queryBuilder;
    private final OpenAireStatsToolClient client;
    private final CacheService<String, DimensionMembers> cache;

    public OpenAireDimensionMemberFetcher(
            OpenAireQueryBuilder queryBuilder, OpenAireStatsToolClient client, CacheService<String, DimensionMembers> cache) {
        this.queryBuilder = queryBuilder;
        this.client = client;
        this.cache = cache;
    }

    DimensionMembers fetchCached(String cacheKey, String groupDimensionCode, String resultType) {
        DimensionMembers cached = cache.fetch(cacheKey);
        if (cached != null) {
            return cached;
        }
        DimensionMembers fetched = fetchLive(groupDimensionCode, resultType);
        cache.save(cacheKey, fetched, CACHE_TTL);
        return fetched;
    }

    private DimensionMembers fetchLive(String groupDimensionCode, String resultType) {
        var subQuery = queryBuilder.resultCountByGroupSubQuery(
            REFERENCE_YEAR, List.of(), resultType, groupDimensionCode, DimensionFilters.NONE);
        var raw = client.fetchRaw(queryBuilder.seriesRequest(List.of(subQuery)));
        List<DimensionMember> members = raw.get(0).stream()
            .map(row -> new DimensionMember(row.get(1), row.get(1)))
            .toList();
        return new DimensionMembers(members);
    }
}
