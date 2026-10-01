package eu.openaire.observatory.indicator.survey;

import eu.openaire.observatory.statistics.StatsToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Statuses that mean "the survey stats-tool has no data for this named query", not a failure.
     * The tool answers an unregistered/unavailable {@code eosc.sb.<year>.<question>} with a 404 or
     * (observed live) a 422 — the request shape is fixed and always valid, so a 422 here can only
     * mean the named query can't be resolved.
     */
    private static final Set<Integer> NO_DATA_STATUSES =
        Set.of(HttpStatus.NOT_FOUND.value(), HttpStatus.UNPROCESSABLE_ENTITY.value());

    private final String endpoint;
    private final WebClient webClient;

    public SurveyStatsToolClient(WebClient.Builder webClientBuilder, StatsToolProperties statsToolProperties) {
        this.endpoint = statsToolProperties.getEndpoint();
        this.webClient = webClientBuilder.baseUrl(this.endpoint).build();
    }

    /**
     * Rows of the single requested series (e.g. {@code [countryCode, value]}, or {@code [value]} for
     * a {@code .sum} query), or {@link Optional#empty()} when the survey stats-tool has <em>no
     * data</em> for this named query — an unregistered {@code eosc.sb.<year>.<question>}, surfaced as
     * an HTTP 404 or 422, a 200 with an empty/absent {@code datasets} array, or a null
     * {@code series}/{@code result}. An answered survey that produced zero rows returns
     * {@code Optional.of(List.of())} (present but empty).
     *
     * <p>A genuine failure — any 5xx, or any 4xx other than 404/422 (e.g. 400/401/403, which point
     * at a malformed request or a misconfigured endpoint/profile) — still throws
     * {@link SurveyStatsToolQueryException}, as does a malformed request URI.
     *
     * <p>TODO: no access check is applied here, so questions configured as restricted in the
     * stats-tool {@code query-access} settings (e.g. {@code eosc.sb.2021.Question6}) are fetched for
     * any caller. Authorize each {@code eosc.sb.<year>.<question>} against the actor before fetching
     * once real authorization is enabled.
     */
    public Optional<List<List<String>>> fetchRowsIfAvailable(String json) {
        URI uri = rawUri(json);
        logger.trace("Survey stats-tool request: {}", uri);
        SurveyRawResponse response = webClient.get()
            .uri(uri)
            .exchangeToMono(clientResponse -> {
                HttpStatusCode status = clientResponse.statusCode();
                if (NO_DATA_STATUSES.contains(status.value())) {
                    return clientResponse.releaseBody().then(Mono.<SurveyRawResponse>empty());
                }
                if (status.isError()) {
                    return clientResponse.bodyToMono(String.class).defaultIfEmpty("")
                        .flatMap(body -> Mono.<SurveyRawResponse>error(new SurveyStatsToolQueryException(
                            "Survey stats-tool returned an error status [%s]: %s".formatted(status, body))));
                }
                return clientResponse.bodyToMono(SurveyRawResponse.class);
            })
            .onErrorMap(WebClientRequestException.class, e -> new SurveyStatsToolQueryException(
                "Survey stats-tool is unreachable: " + e.getMessage(), e))
            .block();
        if (response == null || response.datasets() == null || response.datasets().isEmpty()) {
            return Optional.empty();
        }
        SurveyRawResponse.Series series = response.datasets().get(0).series();
        if (series == null || series.result() == null) {
            return Optional.empty();
        }
        return Optional.of(series.result().stream().map(SurveyRawResponse.RowWrapper::row).toList());
    }

    /**
     * Rows of the single requested series. Prefer {@link #fetchRowsIfAvailable} — this variant
     * throws {@link SurveyStatsToolQueryException} when the tool has no data for the query, so it
     * is only appropriate for callers that require the series to exist.
     */
    public List<List<String>> fetchRows(String json) {
        return fetchRowsIfAvailable(json)
            .orElseThrow(() -> new SurveyStatsToolQueryException(
                "Survey stats-tool returned no data for query: " + json));
    }

    private URI rawUri(String json) {
        try {
            return new URI(endpoint + "raw?json=" + URLEncoder.encode(json, StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new SurveyStatsToolQueryException("Failed to build survey stats-tool request URI", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }
}
