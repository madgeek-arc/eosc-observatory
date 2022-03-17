package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;

import java.util.Map;
import java.util.TreeMap;

public class SurveyAnswer implements Identifiable<String> {

    private String id;
    private String surveyId;
    private String stakeholderId;
    private String type;
    private Map<String, ChapterAnswer> chapterAnswers;
    private Metadata metadata;
    private History history;
    private boolean validated;
    private boolean published;

    public SurveyAnswer() {
        this.chapterAnswers = new TreeMap<>();
        this.metadata = new Metadata();
        this.history = new History();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(String surveyId) {
        this.surveyId = surveyId;
    }

    public String getStakeholderId() {
        return stakeholderId;
    }

    public void setStakeholderId(String stakeholderId) {
        this.stakeholderId = stakeholderId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, ChapterAnswer> getChapterAnswers() {
        return chapterAnswers;
    }

    public void setChapterAnswers(Map<String, ChapterAnswer> chapterAnswers) {
        this.chapterAnswers = chapterAnswers;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}
