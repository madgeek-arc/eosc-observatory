package eu.openaire.observatory.resources.analyzer.model;

import java.util.LinkedHashSet;

public class UrlReferences {

    private String url;
    private LinkedHashSet<SurveyAnswerReference> references;

    public UrlReferences() {
        // no-arg constructor
    }

    public UrlReferences(String url, LinkedHashSet<SurveyAnswerReference> references) {
        this.url = url;
        this.references = references;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LinkedHashSet<SurveyAnswerReference> getReferences() {
        return references;
    }

    public void setReferences(LinkedHashSet<SurveyAnswerReference> surveyAnswerReferences) {
        this.references = surveyAnswerReferences;
    }
}
