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

package eu.openaire.observatory.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping(path = "analytics", produces = MediaType.APPLICATION_JSON_VALUE)
public class AnalyticsController {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String tokenAuth;

    public AnalyticsController(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                               @Value("${analytics.url}") String url,
                               @Value("${analytics.token}") String token) {
        this.webClient = webClientBuilder.baseUrl(url).build();
        this.tokenAuth = token;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/pageviews")
    public ResponseEntity<CountryPageviewsResponse> getCountryPageviews(@RequestParam(required = false) String country,
                                                                        @RequestParam int months) throws Exception {

        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(months);
        Map<YearMonth, Integer> monthHits = new LinkedHashMap<>();
        for (int i = 1; i <= months; i++) {
            monthHits.put(startMonth.plusMonths(i), 0);
        }

        for (YearMonth ym : monthHits.keySet()) {
            String dateStr = ym.toString();
            String topPagesJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/index.php")
                            .queryParam("module", "API")
                            .queryParam("method", "Actions.getPageUrls")
                            .queryParam("idSite", 1)
                            .queryParam("period", "month")
                            .queryParam("date", dateStr)
                            .queryParam("format", "JSON")
                            .queryParam("filter_limit", -1)
                            .queryParam("token_auth", tokenAuth)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<PageEntry> topPages = objectMapper.readValue(topPagesJson, new TypeReference<>() {
            });

            Optional<PageEntry> countryPage = topPages.stream()
                    .filter(p -> "country".equals(p.label()))
                    .findFirst();

            if (countryPage.isPresent()) {
                int idSubtable = countryPage.get().idSubtable();
                String countryJson = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/index.php")
                                .queryParam("module", "API")
                                .queryParam("method", "Actions.getPageUrls")
                                .queryParam("idSite", 1)
                                .queryParam("period", "month")
                                .queryParam("date", dateStr)
                                .queryParam("idSubtable", idSubtable)
                                .queryParam("format", "JSON")
                                .queryParam("filter_limit", -1)
                                .queryParam("token_auth", tokenAuth)
                                .build())
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                List<CountryEntry> countries =
                        objectMapper.readValue(countryJson, new TypeReference<>() {
                        });

                countries.stream()
                        .filter(c -> country == null || country.equalsIgnoreCase(c.label()))
                        .forEach(c -> monthHits.computeIfPresent(ym, (k, v) -> v + c.nbHits()));
            }
        }

        LinkedHashMap<String, Integer> result = monthHits.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                + " " + e.getKey().getYear(),
                        Map.Entry::getValue,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        CountryPageviewsResponse response = new CountryPageviewsResponse(country, result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pageviews/{*path}")
    public ResponseEntity<PageviewsPerMonthResponse> getPageviewsByPath(@PathVariable("path") String path,
                                                                        @RequestParam int months) throws Exception {

        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(months);
        Map<YearMonth, Integer> monthHits = new LinkedHashMap<>();
        for (int i = 1; i <= months; i++) {
            monthHits.put(startMonth.plusMonths(i), 0);
        }

        for (YearMonth ym : monthHits.keySet()) {
            String dateStr = ym.toString();
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/index.php")
                            .queryParam("module", "API")
                            .queryParam("method", "Actions.getPageUrls")
                            .queryParam("idSite", 1)
                            .queryParam("period", "month")
                            .queryParam("date", dateStr)
                            .queryParam("format", "JSON")
                            .queryParam("filter_limit", -1)
                            .queryParam("flat", 1)
                            .queryParam("filter_pattern", path)
                            .queryParam("token_auth", tokenAuth)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            List<PageEntry> pages = objectMapper.readValue(json, new TypeReference<>() {
            });
            int total = pages.stream()
                    .filter(p -> p.label().startsWith(path))
                    .mapToInt(PageEntry::nbHits)
                    .sum();
            monthHits.put(ym, total);
        }

        LinkedHashMap<String, Integer> result = monthHits.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                                + " " + e.getKey().getYear(),
                        Map.Entry::getValue,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));

        return ResponseEntity.ok(new PageviewsPerMonthResponse(result));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageEntry(
            String label,
            @JsonProperty("idsubdatatable") int idSubtable,
            @JsonProperty("nb_hits") int nbHits) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CountryEntry(
            String label,
            @JsonProperty("nb_hits") int nbHits) {
    }

    public record CountryPageviewsResponse(String country, LinkedHashMap<String, Integer> pageviewsPerMonth) {
    }

    public record PageviewsPerMonthResponse(LinkedHashMap<String, Integer> pageviewsPerMonth) {
    }

}