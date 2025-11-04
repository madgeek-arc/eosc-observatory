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
package eu.openaire.observatory.commenting.domain;

import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comment", schema = "commenting")
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    private UUID id = UUID.randomUUID();

    private String path;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private CommentType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private CommentStatus status;

    @OneToMany(
            mappedBy = "comment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("createdAt ASC")
    private List<CommentMessage> messages = new ArrayList<>();

    @Version
    private Long version;

    public Comment() {
        // no-arg constructor
    }

    public Comment(UUID id) {
        this.id = id;
    }

    // region Helper Methods
    public void addMessage(CommentMessage message) {
        messages.add(message);
        message.setComment(this);
    }

    public void removeMessage(CommentMessage message) {
        messages.remove(message);
        message.setComment(null);
    }
    // endregion

    // region Getters/Setters
    public UUID getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public CommentType getType() {
        return type;
    }

    public void setType(CommentType type) {
        this.type = type;
    }

    public CommentStatus getStatus() {
        return status;
    }

    public void setStatus(CommentStatus status) {
        this.status = status;
    }

    public List<CommentMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<CommentMessage> messages) {
        this.messages = messages;
    }

    public Long getVersion() {
        return version;
    }
    // endregion

}
