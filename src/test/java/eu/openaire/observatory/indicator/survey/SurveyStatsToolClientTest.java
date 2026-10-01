package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.statistics.StatsToolProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No live network: the outbound WebClient call is stubbed via an exchangeFunction, same pattern as
 * SurveyStatsToolIndicatorQueryHandlerTest.
 */
class SurveyStatsToolClientTest {

    @Test
    void returnsRowsForAPopulatedSeries() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.OK,
            "{\"datasets\":[{\"series\":{\"result\":[{\"row\":[\"GR\",\"Yes\"]},{\"row\":[\"DE\",\"No\"]}]}}]}");

        Optional<List<List<String>>> rows = client.fetchRowsIfAvailable("{}");

        assertThat(rows).isPresent();
        assertThat(rows.get()).containsExactly(List.of("GR", "Yes"), List.of("DE", "No"));
    }

    @Test
    void returnsPresentEmptyListForAnAnsweredSurveyWithZeroRows() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.OK,
            "{\"datasets\":[{\"series\":{\"result\":[]}}]}");

        assertThat(client.fetchRowsIfAvailable("{}")).contains(List.of());
    }

    @Test
    void returnsEmptyForAnEmptyDatasetsEnvelope() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.OK, "{\"datasets\":[]}");

        assertThat(client.fetchRowsIfAvailable("{}")).isEmpty();
    }

    @Test
    void returnsEmptyForANullSeries() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.OK, "{\"datasets\":[{\"series\":null}]}");

        assertThat(client.fetchRowsIfAvailable("{}")).isEmpty();
    }

    @Test
    void returnsEmptyForANullResult() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.OK, "{\"datasets\":[{\"series\":{\"result\":null}}]}");

        assertThat(client.fetchRowsIfAvailable("{}")).isEmpty();
    }

    @Test
    void returnsEmptyForHttp404() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.NOT_FOUND, "not found");

        assertThat(client.fetchRowsIfAvailable("{}")).isEmpty();
    }

    @Test
    void returnsEmptyForHttp422() {
        // The live survey stats-tool answers an unavailable eosc.sb.<year>.<question> with 422, not 404.
        SurveyStatsToolClient client = clientReturning(HttpStatus.UNPROCESSABLE_ENTITY, "");

        assertThat(client.fetchRowsIfAvailable("{}")).isEmpty();
    }

    @Test
    void throwsForHttp500() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.INTERNAL_SERVER_ERROR, "boom");

        assertThatThrownBy(() -> client.fetchRowsIfAvailable("{}"))
            .isInstanceOf(SurveyStatsToolQueryException.class);
    }

    @Test
    void throwsForHttp400() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.BAD_REQUEST, "bad request");

        assertThatThrownBy(() -> client.fetchRowsIfAvailable("{}"))
            .isInstanceOf(SurveyStatsToolQueryException.class);
    }

    @Test
    void upstreamFailuresMapToBadGateway() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.INTERNAL_SERVER_ERROR, "boom");

        assertThatThrownBy(() -> client.fetchRowsIfAvailable("{}"))
            .isInstanceOfSatisfying(SurveyStatsToolQueryException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void anUnreachableEndpointMapsToBadGateway() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request ->
            Mono.error(new org.springframework.web.reactive.function.client.WebClientRequestException(
                new java.net.ConnectException("refused"), org.springframework.http.HttpMethod.GET,
                request.url(), request.headers())));
        SurveyStatsToolClient client = new SurveyStatsToolClient(builder, new StatsToolProperties()
            .setEndpoint("https://survey-stats.test/stats-api/").setSurveyProfile("eosc-obs"));

        assertThatThrownBy(() -> client.fetchRowsIfAvailable("{}"))
            .isInstanceOfSatisfying(SurveyStatsToolQueryException.class, e -> {
                assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                assertThat(e.getMessage()).contains("unreachable");
            });
    }

    @Test
    void fetchRowsWrapperThrowsWhenThereIsNoData() {
        SurveyStatsToolClient client = clientReturning(HttpStatus.OK, "{\"datasets\":[]}");

        assertThatThrownBy(() -> client.fetchRows("{}"))
            .isInstanceOf(SurveyStatsToolQueryException.class);
    }

    private static SurveyStatsToolClient clientReturning(HttpStatus status, String body) {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> {
            ClientResponse response = ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
            return Mono.just(response);
        });
        StatsToolProperties properties = new StatsToolProperties()
            .setEndpoint("https://survey-stats.test/stats-api/")
            .setSurveyProfile("eosc-obs");
        return new SurveyStatsToolClient(builder, properties);
    }
}
