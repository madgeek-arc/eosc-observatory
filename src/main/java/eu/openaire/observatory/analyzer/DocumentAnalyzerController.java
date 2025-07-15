package eu.openaire.observatory.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.analyzer.model.UrlReferences;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/resources-registry")
public class DocumentAnalyzerController {

    private final SurveyAnswerDocumentAnalyzer registryService;

    public DocumentAnalyzerController(SurveyAnswerDocumentAnalyzer registryService) {
        this.registryService = registryService;
    }

    @GetMapping("survey-answer/{id}/analyze")
    public JsonNode data(@PathVariable String id) {
        return registryService.generateDocuments(id);
    }

    @GetMapping("survey-answer/{id}")
    public List<UrlReferences> extractUrlsFromAnswer(@PathVariable String id) {
        return registryService.extractUrlsFromSurveyAnswer(id);
    }

}
