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

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "mention", schema = "commenting")
public class MessageMention {

    @EmbeddedId
    private MessageMentionId id;

    @JsonBackReference
    @MapsId("messageId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_message_id", nullable = false)
    private CommentMessage commentMessage;

    protected MessageMention() {
        // no-arg constructor
    }

    public MessageMention(MessageMentionId id, CommentMessage commentMessage) {
        this.id = id;
        this.commentMessage = commentMessage;
    }

    public MessageMentionId getId() {
        return id;
    }

    public String getUserId() {
        return id.userId;
    }

    public void setUserId(String userId) {
        if (id == null) {
            id = new MessageMentionId();
        }
        this.id.userId = userId;
    }

    public UUID getMessageId() {
        return id.messageId;
    }

    public void setMessageId(UUID messageId) {
        if (id == null) {
            id = new MessageMentionId();
        }
        this.id.messageId = messageId;
    }

    public CommentMessage getCommentMessage() {
        return commentMessage;
    }

    @Embeddable
    public static class MessageMentionId implements Serializable {

        @Column(name = "message_id", nullable = false)
        UUID messageId;

        @Column(name = "user_id", nullable = false)
        String userId;

        protected MessageMentionId() {
            // no-arg constructor
        }

        public MessageMentionId(UUID messageId, String userId) {
            this.messageId = messageId;
            this.userId = userId;
        }

        public UUID getMessageId() {
            return messageId;
        }

        public void setMessageId(UUID messageId) {
            this.messageId = messageId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageMentionId that = (MessageMentionId) o;
            return Objects.equals(messageId, that.messageId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, userId);
        }
    }
}
