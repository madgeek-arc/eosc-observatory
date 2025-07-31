package eu.openaire.observatory.resources.model;

import com.fasterxml.jackson.databind.JsonNode;
import eu.openaire.observatory.domain.Metadata;
import eu.openaire.observatory.resources.analyzer.model.SurveyAnswerReference;

import java.util.*;

public class Document {

    String id;
    String url;
    JsonNode docInfo;
    Metadata metadata;
    String status;
    String source;
    boolean curated = false;
    String text;
    List<String> paragraphs;
    List<String> paragraphsEn;
    List<String> sentences;
    List<String> sentencesEn;
    LinkedHashSet<SurveyAnswerReference> references;

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    public enum Source {
        EXTERNAL,
        SURVEY,
        HARVESTED
    }

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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<String> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<String> paragraphs) {
        this.paragraphs = paragraphs;
    }

    public List<String> getParagraphsEn() {
        return paragraphsEn;
    }

    public void setParagraphsEn(List<String> paragraphsEn) {
        this.paragraphsEn = paragraphsEn;
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
