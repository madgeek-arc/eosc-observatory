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
package eu.openaire.observatory.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.analyzer.model.Document;
import eu.openaire.observatory.analyzer.model.UrlReferences;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/resources-registry", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentAnalyzerController {

    private final SurveyAnswerDocumentAnalyzer registryService;

    public DocumentAnalyzerController(SurveyAnswerDocumentAnalyzer registryService) {
        this.registryService = registryService;
    }

    @PostMapping
    public Document generateFromUrl(@RequestParam String url) {
        return registryService.generateDocument(url);
    }

    @PostMapping("survey-answer/{id}")
    public List<Document> generateDocumentsFromSurveyAnswerId(@PathVariable String id) {
        return registryService.generateDocuments(id);
    }

    @GetMapping("survey-answer/{id}")
    public List<UrlReferences> extractUrlsFromAnswer(@PathVariable String id) {
        return registryService.extractUrlsFromSurveyAnswer(id);
    }

}
