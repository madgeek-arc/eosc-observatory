package eu.openaire.observatory.resources.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import eu.openaire.observatory.resources.model.DocumentMetadata;
import eu.openaire.observatory.service.SurveyAnswerCrudService;
import eu.openaire.documentanalyzer.analyze.service.DocumentAnalyzerService;
import eu.openaire.documentanalyzer.enrich.service.DocumentContentProcessor;
import gr.uoa.di.madgik.catalogue.service.GenericResourceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SurveyAnswerDocumentAnalyzerTest {

    @Mock
    private SurveyAnswerCrudService surveyAnswerCrudService;
    @Mock
    private GenericResourceManager genericResourceManager;
    @Mock
    private UrlExtractor<?> urlExtractor;
    @Mock
    private DocumentTemplateLoader documentTemplateLoader;
    @Mock
    private DocumentContentProcessor documentContentProcessor;
    @Mock
    private DocumentAnalyzerService documentAnalyzerService;

    @Test
    void createMetadataSetsAuditFieldsAndModel() {
        SurveyAnswerDocumentAnalyzer analyzer = analyzer();

        DocumentMetadata metadata = ReflectionTestUtils.invokeMethod(analyzer, "createMetadata", "system", "gpt-test");

        assertThat(metadata).isNotNull();
        assertThat(metadata.getCreatedBy()).isEqualTo("system");
        assertThat(metadata.getModifiedBy()).isEqualTo("system");
        assertThat(metadata.getModel()).isEqualTo("gpt-test");
        assertThat(metadata.getCreationDate()).isNotNull();
        assertThat(metadata.getModificationDate()).isNotNull();
    }

    @Test
    void updateMetadataRefreshesModifierAndModel() {
        SurveyAnswerDocumentAnalyzer analyzer = analyzer();
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setCreatedBy("creator");

        ReflectionTestUtils.invokeMethod(analyzer, "updateMetadata", "editor", metadata, "gpt-test");

        assertThat(metadata.getCreatedBy()).isEqualTo("creator");
        assertThat(metadata.getModifiedBy()).isEqualTo("editor");
        assertThat(metadata.getModel()).isEqualTo("gpt-test");
        assertThat(metadata.getModificationDate()).isNotNull();
    }

    @Test
    void getSentencesSplitsMultilineTextIntoSentences() {
        SurveyAnswerDocumentAnalyzer analyzer = analyzer();

        ArrayNode sentences = ReflectionTestUtils.invokeMethod(
                analyzer,
                "getSentences",
                "First sentence. Second sentence!\nThird line?\nFourth"
        );

        assertThat(sentences).extracting(node -> node.asText())
                .containsExactly("First sentence.", "Second sentence", "Third line", "Fourth");
    }

    private SurveyAnswerDocumentAnalyzer analyzer() {
        SurveyAnswerDocumentAnalyzer analyzer = new SurveyAnswerDocumentAnalyzer(
                surveyAnswerCrudService,
                genericResourceManager,
                new ObjectMapper(),
                (UrlExtractor) urlExtractor,
                documentTemplateLoader,
                documentContentProcessor,
                documentAnalyzerService
        );
        ReflectionTestUtils.setField(analyzer, "model", "gpt-test");
        return analyzer;
    }
}
