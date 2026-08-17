package eu.openaire.observatory.indicator.dimension;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.indicator.IndicatorCodes;
import eu.openaire.observatory.indicator.openaire.OpenAireQueryBuilder;
import eu.openaire.observatory.indicator.openaire.OpenAireStatsToolClient;
import eu.openaire.observatory.service.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same live-request-mocking pattern as OpenAireStatsToolIndicatorQueryHandlerTest — a fake
 * WebClient ExchangeFunction stands in for the real OpenAIRE stats-tool call.
 */
class OpenAireDimensionMemberFetcherTest {

    private final List<String> capturedRequests = new ArrayList<>();
    private final FakeCacheService cache = new FakeCacheService();

    @Test
    void firstSearchFetchesLiveAndPopulatesTheCache() {
        var fetcher = fetcherReturning("{\"data\":[[[\"1985015\",\"Article\"],[\"66287\",\"Book\"]]]}");

        DimensionMembers members = fetcher.fetchCached("key", IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        assertThat(members.members()).containsExactlyInAnyOrder(
            new DimensionMember("Article", "Article"), new DimensionMember("Book", "Book")
        );
        assertThat(capturedRequests).hasSize(1);
        assertThat(cache.store).containsKey("key");
    }

    @Test
    void secondSearchWithTheSameKeyReusesTheCacheWithoutARepeatCall() {
        var fetcher = fetcherReturning("{\"data\":[[[\"1985015\",\"Article\"]]]}");

        fetcher.fetchCached("key", IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);
        fetcher.fetchCached("key", IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        assertThat(capturedRequests).hasSize(1);
    }

    @Test
    void cachesTheFetchedValueWithADailyTtl() {
        var fetcher = fetcherReturning("{\"data\":[[[\"1985015\",\"Article\"]]]}");

        fetcher.fetchCached("key", IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_PUBLICATION);

        assertThat(cache.ttlByKey).containsEntry("key", Duration.ofDays(1));
    }

    @Test
    void queriesTheGivenGroupDimensionAndResultTypeForTheMostRecentCompleteYear() {
        var fetcher = fetcherReturning("{\"data\":[[[\"648\",\"Dataset\"]]]}");

        fetcher.fetchCached("key", IndicatorCodes.CLASSIFICATION_DIMENSION, OpenAireQueryBuilder.RESULT_TYPE_DATASET);

        String request = capturedRequests.get(0);
        assertThat(request).contains("\"field\":\"result.classification\"");
        assertThat(request).contains("\"values\":[\"dataset\"]");
        assertThat(request).contains("\"values\":[\"" + (Year.now().getValue() - 1) + "\"]");
    }

    private OpenAireDimensionMemberFetcher fetcherReturning(String responseJson) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            String rawJson = request.url().getRawQuery().substring("json=".length());
            capturedRequests.add(URLDecoder.decode(rawJson, StandardCharsets.UTF_8));
            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(responseJson)
                .build();
            return Mono.just(response);
        });
        OpenAireStatsToolClient client = new OpenAireStatsToolClient(builder, "https://openaire.test/stats-tool/");
        return new OpenAireDimensionMemberFetcher(new OpenAireQueryBuilder(new ObjectMapper()), client, cache);
    }

    /** Minimal in-memory CacheService fake, tracking the TTL each key was last saved with. */
    private static class FakeCacheService implements CacheService<String, DimensionMembers> {
        private final Map<String, DimensionMembers> store = new HashMap<>();
        private final Map<String, Duration> ttlByKey = new HashMap<>();

        @Override
        public Set<String> fetchKeys(String pattern) {
            return store.keySet();
        }

        @Override
        public boolean containsKey(String key) {
            return store.containsKey(key);
        }

        @Override
        public DimensionMembers fetch(String key) {
            return store.get(key);
        }

        @Override
        public DimensionMembers save(String key, DimensionMembers value) {
            return store.put(key, value);
        }

        @Override
        public DimensionMembers save(String key, DimensionMembers value, Duration ttl) {
            ttlByKey.put(key, ttl);
            return store.put(key, value);
        }

        @Override
        public DimensionMembers remove(String key) {
            return store.remove(key);
        }
    }
}
