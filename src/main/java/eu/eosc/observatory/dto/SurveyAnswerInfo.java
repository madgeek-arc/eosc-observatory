package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.HistoryEntry;
import eu.eosc.observatory.domain.SurveyAnswer;
import gr.athenarc.catalogue.ui.domain.Survey;

import java.util.*;
import java.util.stream.Collectors;

public class SurveyAnswerInfo {

    String surveyAnswerId;
    String surveyId;
    String surveyName;
    boolean validated;
    boolean published;
    Date lastUpdate;
    List<String> editedBy;
    Progress progressRequired;
    Progress progressTotal;
    StakeholderInfo stakeholder;

    public SurveyAnswerInfo() {}

    public static SurveyAnswerInfo composeFrom(SurveyAnswer answer, Survey survey, StakeholderInfo stakeholderInfo) {
        SurveyAnswerInfo info = new SurveyAnswerInfo();
        info.setSurveyAnswerId(answer.getId());
        info.setSurveyId(survey.getId());
        info.setSurveyName(survey.getName());
        info.setValidated(answer.isValidated());
        info.setPublished(answer.isPublished());
        info.setStakeholder(stakeholderInfo);
        info.setLastUpdate(answer.getMetadata().getModificationDate());
        Set<String> editedBy = answer.getHistory().getEntries().stream().map(HistoryEntry::getUserId).collect(Collectors.toSet());
        info.setEditedBy(new ArrayList<>(editedBy));
        return info;
    }

    public String getSurveyAnswerId() {
        return surveyAnswerId;
    }

    public void setSurveyAnswerId(String surveyAnswerId) {
        this.surveyAnswerId = surveyAnswerId;
    }

    public String getSurveyId() {
        return surveyId;
    }

    public void setSurveyId(String surveyId) {
        this.surveyId = surveyId;
    }

    public String getSurveyName() {
        return surveyName;
    }

    public void setSurveyName(String surveyName) {
        this.surveyName = surveyName;
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

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<String> getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(List<String> editedBy) {
        this.editedBy = editedBy;
    }

    public Progress getProgressRequired() {
        return progressRequired;
    }

    public void setProgressRequired(Progress progressRequired) {
        this.progressRequired = progressRequired;
    }

    public Progress getProgressTotal() {
        return progressTotal;
    }

    public void setProgressTotal(Progress progressTotal) {
        this.progressTotal = progressTotal;
    }

    public StakeholderInfo getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(StakeholderInfo stakeholder) {
        this.stakeholder = stakeholder;
    }
}
