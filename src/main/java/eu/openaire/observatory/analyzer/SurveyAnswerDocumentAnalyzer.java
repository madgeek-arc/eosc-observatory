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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.documentanalyzer.analyze.service.DocumentAnalyzerService;
import eu.openaire.documentanalyzer.common.model.Content;
import eu.openaire.documentanalyzer.enrich.service.DocumentContentProcessor;
import eu.openaire.observatory.analyzer.model.Reference;
import eu.openaire.observatory.analyzer.model.SurveyAnswerReference;
import eu.openaire.observatory.analyzer.model.UrlReferences;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.service.SurveyAnswerCrudService;
import gr.uoa.di.madgik.catalogue.service.GenericResourceManager;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SurveyAnswerDocumentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerDocumentAnalyzer.class);

    private final SurveyAnswerCrudService surveyAnswerCrudService;
    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final UrlExtractor<SurveyAnswer> surveyAnswerUrlExtractor;
    private final DocumentTemplateLoader templateLoader;
    private final DocumentContentProcessor documentContentProcessor;
    private final DocumentAnalyzerService documentAnalyzerService;

    public SurveyAnswerDocumentAnalyzer(SurveyAnswerCrudService surveyAnswerCrudService,
                                        GenericResourceManager genericResourceManager,
                                        UrlExtractor<SurveyAnswer> surveyAnswerUrlExtractor,
                                        DocumentTemplateLoader templateLoader,
                                        DocumentContentProcessor documentContentProcessor,
                                        DocumentAnalyzerService documentAnalyzerService) {
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericResourceService = genericResourceManager;
        this.surveyAnswerUrlExtractor = surveyAnswerUrlExtractor;
        this.templateLoader = templateLoader;
        this.documentContentProcessor = documentContentProcessor;
        this.documentAnalyzerService = documentAnalyzerService;
    }

    public List<UrlReferences> extractUrlsFromSurveyAnswer(String surveyAnswerId) {
        SurveyAnswer answer = surveyAnswerCrudService.get(surveyAnswerId);
        return surveyAnswerUrlExtractor.extract(answer);
    }

    public JsonNode generateDocuments(String surveyAnswerId) {
        ArrayNode documents = mapper.createArrayNode();
        List<UrlReferences> urlReferences = extractUrlsFromSurveyAnswer(surveyAnswerId);
        for (UrlReferences urlReference : urlReferences) {
            ObjectNode document;
            try {
                document = (ObjectNode) mapper.readTree(mapper.writeValueAsBytes(genericResourceService.get("document", DigestUtils.sha256Hex(urlReference.getUrl().getBytes()))));
                Set<Reference> set = new LinkedHashSet<>();
                for (JsonNode node : document.get("references")) {
                    set.add(mapper.convertValue(node, SurveyAnswerReference.class));
                }
                set.addAll(urlReference.getReferences());
                if (!urlReference.getReferences().containsAll(set)) {
                    document.set("references", mapper.convertValue(set, ArrayNode.class));
                    document.set("metadata", updateMetadata("USER_NAME", (ObjectNode) document.get("metadata")));
                    genericResourceService.update("document", document.get("id").asText(), document);
                    documents.add(document);
                }
            } catch (ResourceNotFoundException e) {
                document = (ObjectNode) generateDocument(templateLoader.load(), urlReference.getUrl());
                if (document != null) {
                    document.put("id", DigestUtils.sha256Hex(urlReference.getUrl().getBytes()));
                    document.put("url", urlReference.getUrl());
                    document.set("metadata", createMetadata("USER_NAME"));
                    document.set("references", mapper.convertValue(urlReference.getReferences(), ArrayNode.class));
                    genericResourceService.add("document", document);
                    documents.add(document);
                } else {
                    logger.warn("Problem with url: {}", urlReference.getUrl());
                }
            } catch (InvocationTargetException | NoSuchMethodException | NoSuchFieldException | IOException e) {
                throw new RuntimeException(e);
            }
        }
        return documents;
    }

    public JsonNode generateDocument(JsonNode template, String url) {
        Content content = documentAnalyzerService.read(URI.create(url));
        JsonNode json = documentContentProcessor.generate(template, content);
        ArrayNode sentences = getSentences(content.getText());
        if (json.isObject()) {
            ObjectNode obj = (ObjectNode) json;
            obj.put("text", content.getText());
            obj.set("sentences", sentences);
            if (obj.get("docInfo").get("language").asText().startsWith("en")) {
                obj.set("sentencesEn", sentences);
            } else {
                ArrayNode translatedSentences = documentContentProcessor.translate(sentences, "English");
                obj.set("sentencesEn", translatedSentences);
            }
        }

        return json;
    }

    private JsonNode createMetadata(String user) {
        Date now = new Date();
        Metadata metadata = new Metadata();
        metadata.setCreatedBy(user);
        metadata.setCreationDate(now);
        metadata.setModifiedBy(user);
        metadata.setModificationDate(now);
        try {
            return mapper.readTree(mapper.writeValueAsBytes(metadata));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonNode updateMetadata(String user, ObjectNode metadata) {
        Metadata metadataObject = mapper.convertValue(metadata, Metadata.class);
        Date now = new Date();
        metadataObject.setModifiedBy(user);
        metadataObject.setModificationDate(now);
        try {
            return mapper.readTree(mapper.writeValueAsBytes(metadataObject));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayNode getSentences(String text) {
        // Pattern to match sentences ending in ., ! or ?
        Pattern pattern = Pattern.compile("([^.!?\\n]*[.!?]?\\n?)");
        Matcher matcher = pattern.matcher(text);

        ArrayNode arrayNode = mapper.createArrayNode();

        while (matcher.find()) {
            String sentence = matcher.group(1).trim();
            if (!sentence.isEmpty()) {
                arrayNode.add(sentence);
            }
        }
        return arrayNode;
    }

}
