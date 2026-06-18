/*
 * Copyright 2021-2026 OpenAIRE AMKE
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

package eu.openaire.observatory.domain;

import eu.openaire.observatory.service.Identifiable;

public class SurveyTypeSettings implements Identifiable<String> {

    private String id;
    private String surveyType;
    private boolean notifyOnStart = true;
    private boolean notifyOnEnd = true;
    private boolean notifyOnDeadlineChange = true;
    private boolean notifyOnDeadlineApproaching = true;
    private int deadlineApproachingDays = 7;

    public SurveyTypeSettings() {
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSurveyType() {
        return surveyType;
    }

    public SurveyTypeSettings setSurveyType(String surveyType) {
        this.surveyType = surveyType;
        return this;
    }

    public boolean isNotifyOnStart() {
        return notifyOnStart;
    }

    public SurveyTypeSettings setNotifyOnStart(boolean notifyOnStart) {
        this.notifyOnStart = notifyOnStart;
        return this;
    }

    public boolean isNotifyOnEnd() {
        return notifyOnEnd;
    }

    public SurveyTypeSettings setNotifyOnEnd(boolean notifyOnEnd) {
        this.notifyOnEnd = notifyOnEnd;
        return this;
    }

    public boolean isNotifyOnDeadlineChange() {
        return notifyOnDeadlineChange;
    }

    public SurveyTypeSettings setNotifyOnDeadlineChange(boolean notifyOnDeadlineChange) {
        this.notifyOnDeadlineChange = notifyOnDeadlineChange;
        return this;
    }

    public boolean isNotifyOnDeadlineApproaching() {
        return notifyOnDeadlineApproaching;
    }

    public SurveyTypeSettings setNotifyOnDeadlineApproaching(boolean notifyOnDeadlineApproaching) {
        this.notifyOnDeadlineApproaching = notifyOnDeadlineApproaching;
        return this;
    }

    public int getDeadlineApproachingDays() {
        return deadlineApproachingDays;
    }

    public SurveyTypeSettings setDeadlineApproachingDays(int deadlineApproachingDays) {
        this.deadlineApproachingDays = deadlineApproachingDays;
        return this;
    }
}
