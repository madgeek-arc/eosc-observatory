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
package eu.openaire.observatory.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class History implements Serializable {

    List<HistoryEntry> entries = new ArrayList<>();

    public History() {
        // no-arg constructor
    }

    public List<HistoryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<HistoryEntry> entries) {
        this.entries = entries;
    }

    public void addEntry(String userId, String userRole, String comment, Date date, HistoryAction action) {
        entries.add(new HistoryEntry(userId, userRole, comment, date.getTime(), action));
    }

    public void addEntry(String userId, String userRole, String comment, Date date, HistoryAction action, String version) {
        entries.add(new HistoryEntry(userId, userRole, comment, date.getTime(), action, version));
    }

    public enum HistoryAction {
        CREATED,
        IMPORTED,
        UPDATED,
        VALIDATED,
        INVALIDATED,
        PUBLISHED,
        UNPUBLISHED,
        RESTORED;
    }
}
