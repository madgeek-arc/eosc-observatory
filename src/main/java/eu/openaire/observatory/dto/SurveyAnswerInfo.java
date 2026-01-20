/*
 * Copyright 2021-2025 OpenAIRE AMKE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.dto;

import eu.openaire.observatory.domain.Editor;
import eu.openaire.observatory.domain.SurveyAnswer;
import gr.uoa.di.madgik.catalogue.ui.domain.Model;

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

    public SurveyAnswerInfo() {
        // no-arg constructor
    }

    public static SurveyAnswerInfo composeFrom(SurveyAnswer answer, Model survey, StakeholderInfo stakeholderInfo) {
        SurveyAnswerInfo info = new SurveyAnswerInfo();
        info.setSurveyAnswerId(answer.getId());
        info.setSurveyId(survey.getId());
        info.setSurveyName(survey.getName());
        info.setValidated(answer.isValidated());
        info.setPublished(answer.isPublished());
        info.setStakeholder(stakeholderInfo);
        info.setLastUpdate(answer.getMetadata().getModificationDate());
        Set<String> editedBy = answer
                .getHistory()
                .getEntries()
                .stream()
                .flatMap(he -> Objects.requireNonNullElse(he.getEditors(), new ArrayList<Editor>()) // TODO: add userId as well ??
                        .stream()
                        .map(Editor::getUser)
                )
                .collect(Collectors.toSet());
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
