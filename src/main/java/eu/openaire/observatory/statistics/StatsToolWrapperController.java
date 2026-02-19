/*
 * Copyright 2021-2026 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.openaire.observatory.statistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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
                        HttpStatusCode::isError,
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
