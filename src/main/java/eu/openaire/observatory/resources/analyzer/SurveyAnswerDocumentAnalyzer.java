/*
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
package eu.openaire.observatory.resources.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.openaire.documentanalyzer.analyze.service.DocumentAnalyzerService;
import eu.openaire.documentanalyzer.common.model.Content;
import eu.openaire.documentanalyzer.enrich.service.DocumentContentProcessor;
import eu.openaire.observatory.resources.analyzer.model.GenerateDocumentsRequest;
import eu.openaire.observatory.resources.model.Document;
import eu.openaire.observatory.resources.analyzer.model.SurveyAnswerReference;
import eu.openaire.observatory.resources.analyzer.model.UrlReferences;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.domain.SurveyAnswer;
import eu.openaire.observatory.service.SurveyAnswerCrudService;
import gr.uoa.di.madgik.catalogue.service.GenericResourceManager;
import gr.uoa.di.madgik.catalogue.service.GenericResourceService;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SurveyAnswerDocumentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerDocumentAnalyzer.class);
    private static final String USER = "system";

    private final SurveyAnswerCrudService surveyAnswerCrudService;
    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper;
    private final UrlExtractor<SurveyAnswer> surveyAnswerUrlExtractor;
    private final DocumentTemplateLoader templateLoader;
    private final DocumentContentProcessor documentContentProcessor;
    private final DocumentAnalyzerService documentAnalyzerService;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    public SurveyAnswerDocumentAnalyzer(SurveyAnswerCrudService surveyAnswerCrudService,
                                        GenericResourceManager genericResourceManager,
                                        ObjectMapper mapper,
                                        UrlExtractor<SurveyAnswer> surveyAnswerUrlExtractor,
                                        DocumentTemplateLoader templateLoader,
                                        DocumentContentProcessor documentContentProcessor,
                                        DocumentAnalyzerService documentAnalyzerService) {
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericResourceService = genericResourceManager;
        this.mapper = mapper;
        this.surveyAnswerUrlExtractor = surveyAnswerUrlExtractor;
        this.templateLoader = templateLoader;
        this.documentContentProcessor = documentContentProcessor;
        this.documentAnalyzerService = documentAnalyzerService;
    }

    public List<UrlReferences> extractUrlsFromSurveyAnswer(String surveyAnswerId) {
        SurveyAnswer answer = surveyAnswerCrudService.get(surveyAnswerId);
        return surveyAnswerUrlExtractor.extract(answer);
    }

    @Async
    public void generate(GenerateDocumentsRequest request) {
        List<String> answerIds = request.surveyAnswerIds();
        if (request.surveyAnswerIds().isEmpty()) {
            FacetFilter filter = new FacetFilter();
            filter.setQuantity(1000);
            filter.addFilter("surveyId", request.surveyId());
            answerIds = surveyAnswerCrudService.getAll(filter).getResults().stream().map(SurveyAnswer::getId).toList();
        }
        for (String answerId : answerIds) {
            logger.info("Generating documents for survey answer with id={}", answerId);
            generateDocuments(answerId);
        }
    }

    public List<Document> generateDocuments(String surveyAnswerId) {
        List<Document> documents = new ArrayList<>();
        List<UrlReferences> urlReferences = extractUrlsFromSurveyAnswer(surveyAnswerId);
        for (UrlReferences urlReference : urlReferences) {
            Document document;
            String id = DigestUtils.sha256Hex(urlReference.getUrl().getBytes());
            try {
                LinkedHashSet<SurveyAnswerReference> set = new LinkedHashSet<>();
                document = genericResourceService.get("document", id);
                set.addAll(document.getReferences());
                set.addAll(urlReference.getReferences());

                if (!model.equals(document.getMetadata().getModel())) {
                    Document updated = generateDocument(templateLoader.load(), urlReference.getUrl());
                    if (updated != null) {
                        updated.setId(document.getId());
                        updated.setUrl(urlReference.getUrl());
                        updated.setStatus(Document.Status.PENDING.name());
                        updated.setSource(Document.Source.SURVEY.name());

                        updated.setMetadata(updateMetadata(USER, document.getMetadata(), model));
                        updated.setReferences(set);
                        genericResourceService.update("document", document.getId(), updated);
                        documents.add(updated);
                    }
                } else if (!urlReference.getReferences().containsAll(set)) {
                    set.addAll(urlReference.getReferences());
                    document.setReferences(set);
                    document.setMetadata(updateMetadata(USER, document.getMetadata(), model));
                    genericResourceService.update("document", document.getId(), document);
                    documents.add(document);
                }
            } catch (ResourceNotFoundException e) {
                document = generateDocument(templateLoader.load(), urlReference.getUrl());
                if (document != null) {
                    document.setId(id);
                    document.setUrl(urlReference.getUrl());
                    document.setStatus(Document.Status.PENDING.name());
                    document.setSource(Document.Source.SURVEY.name());
                    document.setMetadata(createMetadata(USER, model));
                    document.setReferences(urlReference.getReferences());
                    genericResourceService.add("document", document);
                    documents.add(document);
                } else {
                    logger.warn("Problem with url: {}", urlReference.getUrl());
                }
            } catch (InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        return documents;
    }

    public Document generateDocument(String url) {
        Document document;
        try {
            document = genericResourceService.get("document", DigestUtils.sha256Hex(url.getBytes()));
        } catch (ResourceNotFoundException e) {
            document = generateDocument(templateLoader.load(), url);
            if (document != null) {
                document.setId(DigestUtils.sha256Hex(url.getBytes()));
                document.setUrl(url);
                document.setStatus(Document.Status.PENDING.name());
                document.setSource(Document.Source.EXTERNAL.name());
                document.setMetadata(new Metadata(SecurityContextHolder.getContext().getAuthentication()));
                genericResourceService.add("document", document);
            } else {
                logger.warn("Problem with url: {}", url);
            }
        }
        return document;
    }

    private Document generateDocument(JsonNode template, String url) {
        Content content;
        try {
            content = documentAnalyzerService.read(URI.create(url));
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        JsonNode json = documentContentProcessor.generate(template, content);
        ArrayNode paragraphs = documentContentProcessor.extractInformation(content);
        ArrayNode sentences = getSentences(content.getText());
        if (json != null && json.isObject() && !json.isEmpty()) {
            ObjectNode obj = (ObjectNode) json;
            obj.put("text", content.getText());
            obj.set("sentences", sentences);
            obj.set("paragraphs", paragraphs);
            if (obj.get("docInfo").get("language").asText().startsWith("en")) {
                obj.set("paragraphsEn", paragraphs);
                obj.set("sentencesEn", sentences);
            } else {
                obj.set("paragraphsEn", documentContentProcessor.translate(paragraphs, "English"));
                obj.set("sentencesEn", documentContentProcessor.translate(sentences, "English"));
            }
//            obj.set("paragraphs", documentContentProcessor.extractInformation(content));
        }

        return mapper.convertValue(json, Document.class);
    }

    private Metadata createMetadata(String user, String model) {
        Date now = new Date();
        Metadata metadata = new Metadata();
        metadata.setCreatedBy(user);
        metadata.setCreationDate(now);
        metadata.setModifiedBy(user);
        metadata.setModificationDate(now);
        metadata.setModel(model);
        return metadata;
    }

    private Metadata updateMetadata(String user, Metadata metadata, String model) {
        Date now = new Date();
        metadata.setModifiedBy(user);
        metadata.setModificationDate(now);
        metadata.setModel(model);
        return metadata;
    }

    private ArrayNode getSentences(String text) {
        String[] texts = text.split("[.?!]?\n+");

        // Pattern to match sentences ending in ., ! or ?
        Pattern pattern = Pattern.compile("((\\.{2,})?[^.!?\\n]*[.!?]?\\n?)");
        ArrayNode arrayNode = mapper.createArrayNode();

        for (String txt : texts) {
            Matcher matcher = pattern.matcher(txt);
            while (matcher.find()) {
                String sentence = matcher.group(1).trim();
                if (!sentence.isEmpty()) {
                    arrayNode.add(sentence);
                }
            }
        }
        return arrayNode;
    }

}
