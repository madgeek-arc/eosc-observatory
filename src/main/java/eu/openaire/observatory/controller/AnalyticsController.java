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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
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

        LocalDate endDate = LocalDate.now(); 
        LocalDate startDate = endDate.minusMonths(months);
        Map<YearMonth, Integer> monthHits = new LinkedHashMap<>();
        for (int i = 0; i <= months; i++) {
            YearMonth ym = YearMonth.from(startDate.plusMonths(i));
            monthHits.put(ym, 0);
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String dateStr = date.toString();
            String topPagesJson = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/index.php")
                            .queryParam("module", "API")
                            .queryParam("method", "Actions.getPageUrls")
                            .queryParam("idSite", 1)
                            .queryParam("period", "day")
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
                    .filter(p -> "country".equals(p.getLabel()))
                    .findFirst();

            if (countryPage.isPresent()) {
                int idSubtable = countryPage.get().getIdSubtable();
                String countryJson = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/index.php")
                                .queryParam("module", "API")
                                .queryParam("method", "Actions.getPageUrls")
                                .queryParam("idSite", 1)
                                .queryParam("period", "day")
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

                LocalDate finalDate = date;
                countries.stream()
                        .filter(c -> country == null || country.equalsIgnoreCase(c.getLabel()))
                        .forEach(c -> {
                            YearMonth ym = YearMonth.from(finalDate);
                            monthHits.computeIfPresent(ym, (k, v) -> v + c.getNbHits());
                        });
            }
        }

        Map<String, Integer> result = monthHits.entrySet().stream()
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

    public static class PageEntry {
        private String label;

        @JsonProperty("idsubdatatable")
        private int idSubtable;

        public String getLabel() {
            return label;
        }

        public int getIdSubtable() {
            return idSubtable;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setIdSubtable(int idSubtable) {
            this.idSubtable = idSubtable;
        }
    }

    public static class CountryEntry {
        private String label;

        @JsonProperty("nb_hits")
        private int nbHits;

        public String getLabel() {
            return label;
        }

        public int getNbHits() {
            return nbHits;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public void setNbHits(int nbHits) {
            this.nbHits = nbHits;
        }
    }

    public static class CountryPageviewsResponse {
        private String country;
        private Map<String, Integer> pageviewsPerMonth;

        public CountryPageviewsResponse(String country, Map<String, Integer> pageviewsPerMonth) {
            this.country = country;
            this.pageviewsPerMonth = pageviewsPerMonth;
        }

        public String getCountry() {
            return country;
        }

        public Map<String, Integer> getPageviewsPerMonth() {
            return pageviewsPerMonth;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public void setPageviewsPerMonth(Map<String, Integer> pageviewsPerMonth) {
            this.pageviewsPerMonth = pageviewsPerMonth;
        }
    }
}