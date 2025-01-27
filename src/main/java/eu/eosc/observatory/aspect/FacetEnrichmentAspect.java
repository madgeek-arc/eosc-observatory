/**
 * Copyright 2021-2025 OpenAIRE AMKE
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
package eu.eosc.observatory.aspect;

import gr.uoa.di.madgik.registry.domain.Browsing;
import gr.uoa.di.madgik.registry.domain.Facet;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.document.DocumentField;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
public class FacetEnrichmentAspect {

    private static final Logger logger = LoggerFactory.getLogger(FacetEnrichmentAspect.class);

    private final RestHighLevelClient client;

    public FacetEnrichmentAspect(RestHighLevelClient client) {
        this.client = client;
    }

    @AfterReturning(pointcut = "within(eu.eosc.observatory.service.SurveyService+) && execution(* browseSurveyAnswersInfo(gr.uoa.di.madgik.registry.domain.FacetFilter) )",
            returning = "browsing")
    public void enrichPagingFacets(Browsing<?> browsing) {
        browsing.setFacets(createLabels(browsing.getFacets()));
    }

    List<Facet> createLabels(List<Facet> facets) {
        Map<String, String> vocabularyValues = getIdNameFields();

        for (Facet facet : facets) {
            facet.getValues().forEach(value -> value.setLabel(getLabelElseKeepValue(value.getValue(), vocabularyValues)));
        }
        return facets;
    }

    private Map<String, String> getIdNameFields() {
        Map<String, String> idNameMap = new TreeMap<>();
        SearchRequest searchRequest = new SearchRequest();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder
                .from(0)
                .size(10000)
                .docValueField("*_id")
                .docValueField("resource_internal_id")
                .docValueField("name")
                .fetchSource(false)
                .explain(true);
        searchRequest.source(searchSourceBuilder);

        SearchResponse response = null;
        try {
            response = client.search(searchRequest, RequestOptions.DEFAULT);

            List<SearchHit> hits = Arrays.stream(response.getHits().getHits()).collect(Collectors.toList());

            for (SearchHit hit : hits) {
                if (hit.getFields().containsKey("resource_internal_id") && hit.getFields().containsKey("name")) {
                    idNameMap.put((String) hit.getFields().get("resource_internal_id").getValues().get(0), (String) hit.getFields().get("name").getValues().get(0));
                } else if (hit.getFields().containsKey("name") && hit.getFields().size() > 1) {
                    String name = (String) hit.getFields().remove("name").getValues().get(0);
                    List<DocumentField> id = (List<DocumentField>) hit.getFields().values();
                    idNameMap.put((String) id.get(0).getValues().get(0), name);
                }
            }
        } catch (IOException e) {
            logger.error("Error retrieving Id / Name values from all resources.", e);
        }
        return idNameMap;
    }

    String getLabelElseKeepValue(String value, Map<String, String> labels) {
        String ret = labels.get(value);
        if (ret == null) {
            ret = toProperCase(toProperCase(value, "-", "-"), "_", " ");
        }
        return ret;
    }

    String toProperCase(String str, String delimiter, String newDelimiter) {
        if (str.equals("")){
            str = "-";
        }
        StringJoiner joiner = new StringJoiner(newDelimiter);
        for (String s : str.split(delimiter)) {
            String s1;
            s1 = s.substring(0, 1).toUpperCase() + s.substring(1);
            joiner.add(s1);
        }
        return joiner.toString();
    }

    static class IdName {

        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
