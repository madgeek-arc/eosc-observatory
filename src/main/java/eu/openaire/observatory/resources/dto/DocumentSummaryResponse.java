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

package eu.openaire.observatory.resources.dto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.resources.analyzer.model.SurveyAnswerReference;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.resources.model.DocumentMetadata;
import eu.openaire.observatory.resources.model.Text;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class DocumentSummaryResponse {

    private static final ObjectMapper mapper = new ObjectMapper();
    static {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public record DocumentInfoSummary(
            String title,
            Text shortDescription,
            List<String> organisations,
            List<String> tags) {
    }

    String id;
    String url;
    DocumentInfoSummary docInfo;
    String status;
    boolean curated = false;

    public DocumentSummaryResponse() {
    }

    public DocumentSummaryResponse(Document document) {
        this.id = document.getId();
        this.url = document.getUrl();
        this.docInfo = mapper.convertValue(document.getDocInfo(), DocumentInfoSummary.class);
        this.status = document.getStatus();
        this.curated = document.isCurated();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public DocumentInfoSummary getDocInfo() {
        return docInfo;
    }

    public void setDocInfo(DocumentInfoSummary docInfo) {
        this.docInfo = docInfo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }
}
