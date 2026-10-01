package eu.openaire.observatory.indicator.openaire;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAireStatsToolClientTest {

    @Test
    void aStatsToolThatDoesNotAnswerInTimeMapsToGatewayTimeout() {
        WebClient.Builder builder = WebClient.builder().exchangeFunction(request -> Mono.never());
        OpenAireStatsToolClient client = new OpenAireStatsToolClient(
            builder, "https://openaire.test/stats-tool/", Duration.ofMillis(50));

        assertThatThrownBy(() -> client.fetchRaw("{}"))
            .isInstanceOfSatisfying(OpenAireQueryException.class,
                e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
    }
}
