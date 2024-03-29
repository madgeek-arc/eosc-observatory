package eu.eosc.observatory.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping(path = "statistics", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsToolWrapperController {

    private static final Logger logger = LoggerFactory.getLogger(StatsToolWrapperController.class);
    private final String endpoint;
    private final StatsQuerySecurity statsQuerySecurity;
    private final WebClient webClient;

    public StatsToolWrapperController(@Value("${stats-tool.endpoint}") String endpoint,
                                      StatsQuerySecurity statsQuerySecurity) {
        this.endpoint = endpoint;
        this.statsQuerySecurity = statsQuerySecurity;
        this.webClient = WebClient.builder().baseUrl(this.endpoint).build();
    }

    @PreAuthorize("@statsQuerySecurity.authorize(#json, authentication)")
    @GetMapping(value = "raw")
    public Mono<?> getRawData(@RequestParam(name = "json") String json) {
        return webClient.get()
                .uri(statsUrl("raw", json))
                .retrieve()
                .onStatus(
                        HttpStatus::isError,
                        clientResponse -> clientResponse.bodyToMono(Map.class)
                                .map(response -> {
                                    String errorMgs = String
                                            .format("Stats-tool returned error code. [Error: %s | Message: %s]",
                                                    response.get("error"), response.get("message"));
                                    logger.info(errorMgs);
                                    return new RuntimeException(errorMgs);
                                }))
                .bodyToMono(Map.class);
    }

    URI statsUrl(String path, String json) {
        URI uri;
        try {
            uri = new URI(this.endpoint + path + "?json=" + URLEncoder.encode(json, StandardCharsets.UTF_8));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        logger.trace("stats-tool url: {}", uri);
        return uri;
    }
}
