package eu.openaire.observatory.analyzer.model;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.domain.Metadata;

import java.util.*;

public class Document {

    String id;
    String url;
    JsonNode docInfo;
    Metadata metadata;
    String text;
    List<String> sentences;
    List<String> sentencesEn;
    LinkedHashSet<SurveyAnswerReference> references;

    public Document() {
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

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getSentences() {
        return sentences;
    }

    public void setSentences(List<String> sentences) {
        this.sentences = sentences;
    }

    public List<String> getSentencesEn() {
        return sentencesEn;
    }

    public void setSentencesEn(List<String> sentencesEn) {
        this.sentencesEn = sentencesEn;
    }

    public Set<SurveyAnswerReference> getReferences() {
        return references;
    }

    public void setReferences(LinkedHashSet<SurveyAnswerReference> references) {
        this.references = references;
    }
}
