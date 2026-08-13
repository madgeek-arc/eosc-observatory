package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.statistics.StatsToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Thin synchronous wrapper around the survey stats-tool's "raw?json=" endpoint (a different
 * external system from OpenAIRE's stats-tool — see OpenAireStatsToolClient). Reuses
 * StatsToolProperties for the endpoint rather than a duplicate @Value, since it's already bound
 * (via @ConfigurationProperties) to the same "stats-tool.endpoint" property that
 * StatsToolWrapperController reads through its own separate @Value injection. Does not call
 * StatsToolWrapperController itself — that would be an in-process self-call — this hits the real
 * external endpoint directly, same as OpenAireStatsToolClient does for its own endpoint.
 * IndicatorQueryHandler.execute() is synchronous, so this blocks rather than returning a Mono.
 */
@Component
public class SurveyStatsToolClient {

    private static final Logger logger = LoggerFactory.getLogger(SurveyStatsToolClient.class);

    private final String endpoint;
    private final WebClient webClient;

    public SurveyStatsToolClient(WebClient.Builder webClientBuilder, StatsToolProperties statsToolProperties) {
        this.endpoint = statsToolProperties.getEndpoint();
        this.webClient = webClientBuilder.baseUrl(this.endpoint).build();
    }

    /** Rows of the single requested series, e.g. [countryCode, value] or [value] for a .sum query. */
    public List<List<String>> fetchRows(String json) {
        URI uri = rawUri(json);
        logger.trace("Survey stats-tool request: {}", uri);
        SurveyRawResponse response = webClient.get()
            .uri(uri)
            .retrieve()
            .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                .map(body -> new SurveyStatsToolQueryException(
                    "Survey stats-tool returned an error status [%s]: %s"
                        .formatted(clientResponse.statusCode(), body))))
            .bodyToMono(SurveyRawResponse.class)
            .block();
        if (response == null || response.datasets() == null || response.datasets().isEmpty()) {
            throw new SurveyStatsToolQueryException("Survey stats-tool returned an empty response body");
        }
        SurveyRawResponse.Series series = response.datasets().get(0).series();
        if (series == null || series.result() == null) {
            throw new SurveyStatsToolQueryException("Survey stats-tool response had no result rows");
        }
        return series.result().stream().map(SurveyRawResponse.RowWrapper::row).toList();
    }

    private URI rawUri(String json) {
        try {
            return new URI(endpoint + "raw?json=" + URLEncoder.encode(json, StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new SurveyStatsToolQueryException("Failed to build survey stats-tool request URI", e);
        }
    }
}
