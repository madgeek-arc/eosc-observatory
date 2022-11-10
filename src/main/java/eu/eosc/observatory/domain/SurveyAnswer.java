package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;
import org.json.simple.JSONObject;

public class SurveyAnswer implements Identifiable<String> {

    private String id;
    private String surveyId;
    private String stakeholderId;
    private String type;
    private JSONObject answer;
    private Metadata metadata;
    private History history;
    private boolean validated;
    private boolean published;

    public SurveyAnswer() {
        this.answer = new JSONObject();
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

    public JSONObject getAnswer() {
        return answer;
    }

    public void setAnswer(JSONObject answer) {
        this.answer = answer;
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

    @Override
    public String toString() {
        return "SurveyAnswer{" +
                "id='" + id + '\'' +
                ", surveyId='" + surveyId + '\'' +
                ", stakeholderId='" + stakeholderId + '\'' +
                ", type='" + type + '\'' +
                ", answer=" + answer +
                ", metadata=" + metadata +
                ", history=" + history +
                ", validated=" + validated +
                ", published=" + published +
                '}';
    }
}
