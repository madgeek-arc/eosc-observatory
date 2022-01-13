package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.SurveyAnswer;
import gr.athenarc.catalogue.ui.domain.Survey;

import java.util.Date;
import java.util.List;

public class SurveyAnswerInfo {

    String surveyAnswerId;
    String surveyName;
    boolean validated;
    boolean published;
    Date lastUpdate;
    List<String> editedBy;
    Float progress;
    StakeholderInfo stakeholder;

    public SurveyAnswerInfo() {}

    // TODO: This is a draft.
    //    1. accept List<SurveyAnswer>
    //    2. check all answers before setting Validated/Published
    //    3. editedBy ?? contain separate lists for every SurveyAnswer?
    public static SurveyAnswerInfo composeFrom(SurveyAnswer answer, Survey survey, StakeholderInfo stakeholderInfo) {
        SurveyAnswerInfo info = new SurveyAnswerInfo();
        info.setSurveyAnswerId(answer.getId());
        info.setSurveyName(survey.getName());
        info.setValidated(answer.isValidated());
        info.setPublished(answer.isPublished());
        info.setStakeholder(stakeholderInfo);
        return info;
    }

    public String getSurveyAnswerId() {
        return surveyAnswerId;
    }

    public void setSurveyAnswerId(String surveyAnswerId) {
        this.surveyAnswerId = surveyAnswerId;
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

    public Float getProgress() {
        return progress;
    }

    public void setProgress(Float progress) {
        this.progress = progress;
    }

    public StakeholderInfo getStakeholder() {
        return stakeholder;
    }

    public void setStakeholder(StakeholderInfo stakeholder) {
        this.stakeholder = stakeholder;
    }
}
