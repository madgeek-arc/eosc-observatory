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

package eu.openaire.observatory.commenting.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "comment_message", schema = "commenting")
@EntityListeners(AuditingEntityListener.class)
public class CommentMessage {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    @CreatedBy
    @Column(name = "author_id", nullable = false, updatable = false)
    private String authorId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentThread comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommentMessage parent;

    @OneToMany(mappedBy = "commentMessage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MessageMention> mentions = new ArrayList<>();

    @JsonIgnore
    @Version
    private Long version;

    public CommentMessage() {
        // no-arg constructor
    }

    public CommentMessage(UUID id) {
        this.id = id;
    }

    // region Helper Methods
    // TODO: move this to service
    public void attachReply(CommentMessage reply) {
        reply.setParent(this);
        reply.setComment(this.comment);
        this.getComment().getMessages().add(reply);
    }

    /**
     * Helper method to fill-out message mentions.
     *
     * @param mentionedUsers the user ids mentioned in the message
     */
    public void addMentions(List<String> mentionedUsers) {
        List<MessageMention> mentions = new ArrayList<>();
        if (mentionedUsers != null) {
            for (String mentionedUser : mentionedUsers) {
                MessageMention mention = new MessageMention(this, mentionedUser);
                mentions.add(mention);
            }
        }
        this.mentions = mentions;
    }
    // endregion

    // region Getters/Setters

    public UUID getId() {
        return id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthorId() {
        return authorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public CommentThread getComment() {
        return comment;
    }

    public void setComment(CommentThread comment) {
        this.comment = comment;
    }

    public CommentMessage getParent() {
        return parent;
    }

    public void setParent(CommentMessage parent) {
        this.parent = parent;
    }

    public List<MessageMention> getMentions() {
        return mentions;
    }

    private void setMentions(List<MessageMention> mentions) {
        this.mentions = mentions;
    }

    public Long getVersion() {
        return version;
    }

    // endregion
}
