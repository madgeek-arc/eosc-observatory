/**
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
package eu.openaire.observatory.websockets;

import java.util.Date;

public class SessionActivity {

    String sessionId;
    String fullname;
    String action;
    String position;
    Date date;

    public SessionActivity() {
        // no-arg constructor
        this.date = new Date();
    }

    public SessionActivity(String sessionId) {
        // no-arg constructor
        this.sessionId = sessionId;
        this.date = new Date();
    }

    public SessionActivity(String sessionId, String fullname, String action) {
        this.sessionId = sessionId;
        this.fullname = fullname;
        this.action = action;
        this.date = new Date();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionActivity)) return false;

        SessionActivity activity = (SessionActivity) o;

        return getSessionId() != null ? getSessionId().equals(activity.getSessionId()) : activity.getSessionId() == null;
    }

    @Override
    public int hashCode() {
        return getSessionId() != null ? getSessionId().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SessionActivity{" +
                "sessionId='" + sessionId + '\'' +
                ", fullname='" + fullname + '\'' +
                ", action='" + action + '\'' +
                ", position='" + position + '\'' +
                ", date=" + date +
                '}';
    }
}
