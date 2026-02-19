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

import eu.openaire.observatory.domain.HistoryEntry;
import eu.openaire.observatory.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class HistoryEntryDTO {

    private static final Logger logger = LoggerFactory.getLogger(HistoryEntryDTO.class);

    @Deprecated
    String email;
    @Deprecated
    String fullname;
    @Deprecated
    String role;
    List<EditorDTO> editors = new ArrayList<>();
    String comment;
    long time;
    HistoryActionDTO action;
    String resourceId;
    String version;

    public HistoryEntryDTO() {
        // no-arg constructor
    }

    public static HistoryEntryDTO of(HistoryEntry historyEntry, User user) {
        HistoryEntryDTO entry = new HistoryEntryDTO();
//        if (historyEntry.getUserId() != null && !historyEntry.getUserId().equals(user.getId())) {
//            logger.error("wrong user id");
//        }
        if (historyEntry.getEditors() != null && !historyEntry.getEditors().isEmpty()) {
            entry.setEditors(historyEntry.getEditors().stream().map(u -> new EditorDTO(u.getUser(), u.getRole(), u.getUpdateDate())).toList());
        } else {
            EditorDTO editorDTO = new EditorDTO(historyEntry.getUserId(), historyEntry.getUserRole(), Date.from(Instant.ofEpochSecond(historyEntry.getTime())));
            entry.setEditors(new ArrayList<>());
            entry.getEditors().add(editorDTO);
        }
        entry.setComment(historyEntry.getComment());
        entry.setAction(HistoryActionDTO.of(historyEntry.getAction(), historyEntry.getRegistryVersion()));
        entry.setTime(historyEntry.getTime());

        // --> For backward compatibility reasons
        // TODO: remove
        entry.setEmail(user.getEmail());
        entry.setFullname(user.getFullname());
        entry.setRole(historyEntry.getUserRole());
        // <-- For backward compatibility reasons

        return entry;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<EditorDTO> getEditors() {
        return editors;
    }

    public HistoryEntryDTO setEditors(List<EditorDTO> editors) {
        this.editors = editors;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public HistoryActionDTO getAction() {
        return action;
    }

    public void setAction(HistoryActionDTO action) {
        this.action = action;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
