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

import java.io.Serializable;
import java.util.Date;

public class Revision implements Serializable {

    private String field;
    private Object value;
    private Action action = new Action();
    private String sessionId;
    private Date date = new Date();

    public Revision() {
        // no-arg constructor
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public Revision setAction(Action action) {
        this.action = action;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Revision setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public Revision setDate(Date date) {
        this.date = date;
        return this;
    }
}
