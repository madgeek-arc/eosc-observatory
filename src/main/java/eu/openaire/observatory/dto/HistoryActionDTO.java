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

import eu.openaire.observatory.domain.History;

public class HistoryActionDTO {

    private History.HistoryAction type;
    private String registryVersion;
    private HistoryEntryDTO pointsTo;

    public HistoryActionDTO() {
    }

    public static HistoryActionDTO of(History.HistoryAction type, String registryVersion) {
        HistoryActionDTO actionDTO = new HistoryActionDTO();
        actionDTO.setType(type);
        actionDTO.setRegistryVersion(registryVersion);
        return actionDTO;
    }

    public History.HistoryAction getType() {
        return type;
    }

    public void setType(History.HistoryAction type) {
        this.type = type;
    }

    public String getRegistryVersion() {
        return registryVersion;
    }

    public void setRegistryVersion(String registryVersion) {
        this.registryVersion = registryVersion;
    }

    public HistoryEntryDTO getPointsTo() {
        return pointsTo;
    }

    public void setPointsTo(HistoryEntryDTO pointsTo) {
        this.pointsTo = pointsTo;
    }
}
