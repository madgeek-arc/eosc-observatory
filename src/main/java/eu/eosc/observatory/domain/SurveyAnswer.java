package eu.eosc.observatory.domain;

import eu.eosc.observatory.service.Identifiable;
import org.json.JSONObject;

public class SurveyAnswer implements Identifiable<String> {

    private String surveyId;
    private String stakeholderId;
    private JSONObject answer;
    private Metadata metadata;
    private boolean validated;
    private boolean published;

    public SurveyAnswer() {}

    @Override
    public String getId() {
        return answer.optString("id");
    }

    @Override
    public void setId(String id) {
        this.answer.put("id", id);
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
