package eu.openaire.observatory.analyzer.model;

import java.util.LinkedHashSet;

public class UrlReferences {

    private String url;
    private LinkedHashSet<Reference> references;

    public UrlReferences() {
        // no-arg constructor
    }

    public UrlReferences(String url, LinkedHashSet<Reference> references) {
        this.url = url;
        this.references = references;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LinkedHashSet<Reference> getReferences() {
        return references;
    }

    public void setReferences(LinkedHashSet<Reference> surveyAnswerReferences) {
        this.references = surveyAnswerReferences;
    }
}
