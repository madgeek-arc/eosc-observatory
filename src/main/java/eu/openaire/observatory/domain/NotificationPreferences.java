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
package eu.openaire.observatory.domain;

import java.util.List;

public class NotificationPreferences {

    private boolean emailNotifications = true;
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

    public List<String> getForwardEmails() {
        return forwardEmails;
    }

    public NotificationPreferences setForwardEmails(List<String> forwardEmails) {
        this.forwardEmails = forwardEmails;
        return this;
    }


}
