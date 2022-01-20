package eu.eosc.observatory.dto;

import eu.eosc.observatory.domain.ChapterAnswer;
import eu.eosc.observatory.domain.HistoryEntry;
import eu.eosc.observatory.domain.SurveyAnswer;
import gr.athenarc.catalogue.ui.domain.Survey;
import gr.athenarc.catalogue.ui.domain.UiField;
import org.json.simple.JSONObject;

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

    public static SurveyAnswerInfo composeFrom(SurveyAnswer answer, Survey survey, StakeholderInfo stakeholderInfo, Map<String, List<UiField>> chapterFieldsMap) {
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

        Progress required = new Progress();
        Progress total = new Progress();
        Map<String, JSONObject> chapterAnswers = new HashMap<>();
        for (ChapterAnswer chapterAnswer : answer.getChapterAnswers().values()) {
            chapterAnswers.put(chapterAnswer.getChapterId(), chapterAnswer.getAnswer());
        }
        for (Map.Entry<String, List<UiField>> chapter : chapterFieldsMap.entrySet()) {
            JSONObject chapterAnswer = chapterAnswers.get(chapter.getKey());
            for (UiField field : chapter.getValue()) {
                if (Boolean.FALSE.equals(field.getForm().getDisplay().getVisible())) {
                    continue;
                }
                total.addToTotal(1);
                if (chapterAnswer.get(field.getName()) != null) {
                    total.addToCurrent(1);
                }
                if (Boolean.TRUE.equals(field.getForm().getMandatory())) {
                    required.addToTotal(1);
                    if (chapterAnswer.get(field.getName()) != null) {
                        required.addToCurrent(1);
                    }
                }
            }
        }
        info.setProgressRequired(required);
        info.setProgressTotal(total);

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
