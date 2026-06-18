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

import java.util.List;

public class NotificationPreferences {

    private boolean emailNotifications = true;
    private boolean mentionEmailNotifications = true;
    private boolean surveyEmailNotifications = true;
    private boolean contactFormEmailNotifications = true;
    private List<String> forwardEmails;

    public NotificationPreferences() {
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public NotificationPreferences setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
        return this;
    }

    public boolean isMentionEmailNotifications() {
        return mentionEmailNotifications;
    }

    public NotificationPreferences setMentionEmailNotifications(boolean mentionEmailNotifications) {
        this.mentionEmailNotifications = mentionEmailNotifications;
        return this;
    }

    public boolean isSurveyEmailNotifications() {
        return surveyEmailNotifications;
    }

    public NotificationPreferences setSurveyEmailNotifications(boolean surveyEmailNotifications) {
        this.surveyEmailNotifications = surveyEmailNotifications;
        return this;
    }

    public boolean isContactFormEmailNotifications() {
        return contactFormEmailNotifications;
    }

    public NotificationPreferences setContactFormEmailNotifications(boolean contactFormEmailNotifications) {
        this.contactFormEmailNotifications = contactFormEmailNotifications;
        return this;
    }

    public List<String> getForwardEmails() {
        return forwardEmails;
    }

    public NotificationPreferences setForwardEmails(List<String> forwardEmails) {
        this.forwardEmails = forwardEmails;
        return this;
    }


}
