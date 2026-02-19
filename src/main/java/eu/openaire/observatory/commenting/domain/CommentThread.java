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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comment_thread", schema = "commenting")
@EntityListeners(AuditingEntityListener.class)
public class CommentThread {

    @Id
    private UUID id = UUID.randomUUID();

    @Embedded
    private CommentTarget target;

    @Column(name = "field_id", nullable = false)
    private String fieldId;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private CommentStatus status;

    @JsonManagedReference
    @OneToMany(
            mappedBy = "comment",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    @OrderBy("createdAt ASC")
    private List<CommentMessage> messages = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JsonIgnore
    @Version
    private Long version;

    public CommentThread() {
        // no-arg constructor
    }

    public CommentThread(UUID id) {
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

    public CommentTarget getTarget() {
        return target;
    }

    public void setTarget(CommentTarget target) {
        this.target = target;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String path) {
        this.fieldId = path;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getVersion() {
        return version;
    }
    // endregion

}
