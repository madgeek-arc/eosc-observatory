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

package eu.openaire.observatory.dto;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class HistoryDTO {

    List<HistoryEntryDTO> entries = new ArrayList<>();

    public HistoryDTO() {
        // no-arg constructor
    }

    public HistoryDTO(List<HistoryEntryDTO> entries) {
        this.entries = new LinkedList<>(entries);
    }

    public List<HistoryEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<HistoryEntryDTO> entries) {
        this.entries = entries;
    }
}
