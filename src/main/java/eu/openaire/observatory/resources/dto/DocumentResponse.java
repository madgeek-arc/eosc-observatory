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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.resources.analyzer.model.SurveyAnswerReference;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.resources.model.DocumentMetadata;

import java.util.LinkedHashSet;
import java.util.Set;

public class DocumentResponse {

    private static final ObjectMapper mapper = new ObjectMapper();

    String id;
    String url;
    JsonNode docInfo;
    DocumentMetadata metadata;
    String status;
    String source;
    boolean curated = false;
    LinkedHashSet<SurveyAnswerReference> references;

    public DocumentResponse() {
    }

    public DocumentResponse(Document document) {
        this.id = document.getId();
        this.url = document.getUrl();
        this.metadata = new DocumentMetadata(document.getMetadata());
        this.docInfo = mapper.convertValue(document.getDocInfo(), JsonNode.class);
        this.status = document.getStatus();
        this.source = document.getSource();
        this.curated = document.isCurated();
        this.references = new LinkedHashSet<>(document.getReferences());
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

    public JsonNode getDocInfo() {
        return docInfo;
    }

    public void setDocInfo(JsonNode docInfo) {
        this.docInfo = docInfo;
    }

    public DocumentMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DocumentMetadata metadata) {
        this.metadata = metadata;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isCurated() {
        return curated;
    }

    public void setCurated(boolean curated) {
        this.curated = curated;
    }

    public Set<SurveyAnswerReference> getReferences() {
        return references;
    }

    public void setReferences(LinkedHashSet<SurveyAnswerReference> references) {
        this.references = references;
    }
}
