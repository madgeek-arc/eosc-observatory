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

public class SurveySettings implements Identifiable<String> {

    private String id;
    private String surveyType;
    private boolean notifyOnStart = true;
    private boolean notifyOnEnd = true;
    private boolean notifyOnDeadlineChange = true;
    private boolean notifyOnDeadlineApproaching = true;
    private boolean notifyOnDeadlineDay = true;
    private boolean notifyOnReopened = true;
    private int deadlineApproachingDays = 7;

    public SurveySettings() {
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

    public SurveySettings setSurveyType(String surveyType) {
        this.surveyType = surveyType;
        return this;
    }

    public boolean isNotifyOnStart() {
        return notifyOnStart;
    }

    public SurveySettings setNotifyOnStart(boolean notifyOnStart) {
        this.notifyOnStart = notifyOnStart;
        return this;
    }

    public boolean isNotifyOnEnd() {
        return notifyOnEnd;
    }

    public SurveySettings setNotifyOnEnd(boolean notifyOnEnd) {
        this.notifyOnEnd = notifyOnEnd;
        return this;
    }

    public boolean isNotifyOnDeadlineChange() {
        return notifyOnDeadlineChange;
    }

    public SurveySettings setNotifyOnDeadlineChange(boolean notifyOnDeadlineChange) {
        this.notifyOnDeadlineChange = notifyOnDeadlineChange;
        return this;
    }

    public boolean isNotifyOnDeadlineApproaching() {
        return notifyOnDeadlineApproaching;
    }

    public SurveySettings setNotifyOnDeadlineApproaching(boolean notifyOnDeadlineApproaching) {
        this.notifyOnDeadlineApproaching = notifyOnDeadlineApproaching;
        return this;
    }

    public boolean isNotifyOnDeadlineDay() {
        return notifyOnDeadlineDay;
    }

    public SurveySettings setNotifyOnDeadlineDay(boolean notifyOnDeadlineDay) {
        this.notifyOnDeadlineDay = notifyOnDeadlineDay;
        return this;
    }

    public boolean isNotifyOnReopened() {
        return notifyOnReopened;
    }

    public SurveySettings setNotifyOnReopened(boolean notifyOnReopened) {
        this.notifyOnReopened = notifyOnReopened;
        return this;
    }

    public int getDeadlineApproachingDays() {
        return deadlineApproachingDays;
    }

    public SurveySettings setDeadlineApproachingDays(int deadlineApproachingDays) {
        this.deadlineApproachingDays = deadlineApproachingDays;
        return this;
    }
}
