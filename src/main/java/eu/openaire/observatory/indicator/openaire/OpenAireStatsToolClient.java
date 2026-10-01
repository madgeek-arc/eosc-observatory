package eu.openaire.observatory.indicator.openaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Thin synchronous wrapper around OpenAIRE's stats-tool "raw?json=" endpoint.
 * IndicatorQueryHandler.execute() is synchronous, so this blocks rather than returning a Mono —
 * same precedent as AnalyticsController's matomoApiCall(). URI is built as an absolute java.net.URI
 * (like StatsToolWrapperController.statsUrl()) rather than passed as a String to WebClient's
 * .uri(String), to avoid WebClient re-encoding the already-URL-encoded json parameter.
 */
@Component
public class OpenAireStatsToolClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAireStatsToolClient.class);

    private final String endpoint;
    private final WebClient webClient;
    private final Duration responseTimeout;

    @Autowired
    public OpenAireStatsToolClient(WebClient.Builder webClientBuilder,
                                    @Value("${openaire.stats-tool.endpoint}") String endpoint,
                                    @Value("${openaire.stats-tool.response-timeout:120s}") Duration responseTimeout) {
        this.endpoint = endpoint;
        this.webClient = webClientBuilder.baseUrl(endpoint).build();
        this.responseTimeout = responseTimeout;
    }

    public OpenAireStatsToolClient(WebClient.Builder webClientBuilder, String endpoint) {
        this(webClientBuilder, endpoint, Duration.ofSeconds(120));
    }

    public List<List<List<String>>> fetchRaw(String json) {
        URI uri = rawUri(json);
        logger.trace("OpenAIRE stats-tool request: {}", uri);
        OpenAireRawResponse response = webClient.get()
            .uri(uri)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                .map(body -> new OpenAireQueryException(
                    "OpenAIRE stats-tool returned an error status [%s]: %s"
                        .formatted(clientResponse.statusCode(), body))))
            .bodyToMono(OpenAireRawResponse.class)
            .timeout(responseTimeout)
            .onErrorMap(TimeoutException.class, e -> new OpenAireQueryException(
                "OpenAIRE stats-tool did not answer within " + responseTimeout, HttpStatus.GATEWAY_TIMEOUT, e))
            .onErrorMap(WebClientRequestException.class, e -> new OpenAireQueryException(
                "OpenAIRE stats-tool is unreachable: " + e.getMessage(), e))
            .block();
        if (response == null || response.data() == null) {
            throw new OpenAireQueryException("OpenAIRE stats-tool returned an empty response body");
        }
        return response.data();
    }

    private URI rawUri(String json) {
        try {
            return new URI(endpoint + "raw?json=" + URLEncoder.encode(json, StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new OpenAireQueryException("Failed to build OpenAIRE stats-tool request URI", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
