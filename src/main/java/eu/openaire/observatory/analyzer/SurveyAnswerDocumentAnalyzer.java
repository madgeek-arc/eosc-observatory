package eu.openaire.observatory.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Service
public class SurveyAnswerDocumentAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerDocumentAnalyzer.class);

    private static final String SERVICE_URL = "http://localhost:8666/api/v1/documents/enrich?url=";
    private final SurveyAnswerCrudService surveyAnswerCrudService;
    private final GenericResourceService genericResourceService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final WebClient webClient;
    private final UrlExtractor<SurveyAnswer> surveyAnswerUrlExtractor;

    public SurveyAnswerDocumentAnalyzer(SurveyAnswerCrudService surveyAnswerCrudService,
                                        GenericResourceManager genericResourceManager,
                                        UrlExtractor<SurveyAnswer> surveyAnswerUrlExtractor) {
        this.surveyAnswerCrudService = surveyAnswerCrudService;
        this.genericResourceService = genericResourceManager;
        this.surveyAnswerUrlExtractor = surveyAnswerUrlExtractor;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB
                .build();
    }

    public List<UrlReferences> extractUrlsFromSurveyAnswer(String surveyAnswerId) {
        SurveyAnswer answer = surveyAnswerCrudService.get(surveyAnswerId);
        List<UrlReferences> documentUrls = surveyAnswerUrlExtractor.extract(answer);
        return documentUrls;
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
                document = (ObjectNode) generateDocument(urlReference.getUrl());
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

    public JsonNode generateDocument(String url) {
        ClientResponse response = webClient.get().uri(SERVICE_URL + url).exchange().block();
        if (!response.statusCode().is2xxSuccessful()) {
            return null;
        }
        JsonNode json = response.bodyToMono(JsonNode.class).block();
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

}
