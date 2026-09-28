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

package eu.openaire.observatory.commenting.repository;

import eu.openaire.observatory.commenting.domain.CommentMessage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentMessageRepository extends CrudRepository<CommentMessage, UUID> {

    @EntityGraph(attributePaths = "comment")
    Optional<CommentMessage> findWithCommentById(UUID id);

    @Query("SELECT DISTINCT mm.commentMessage FROM MessageMention mm WHERE mm.id.userId = :userId")
    List<CommentMessage> findMessagesMentioning(@Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("UPDATE CommentMessage m SET m.authorId = :placeholder WHERE m.authorId = :userId")
    void anonymizeAuthor(@Param("userId") String userId, @Param("placeholder") String placeholder);

    /**
     * mention's PK is (message_id, user_id). If this message already has a mention row
     * for the placeholder (e.g. a different mentioned user in the same message was purged
     * earlier), rewriting this user's row to the placeholder would collide on that PK.
     * Drop the redundant row instead — the message already shows the placeholder as mentioned.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM commenting.mention m1 WHERE m1.user_id = :userId " +
                   "AND EXISTS (SELECT 1 FROM commenting.mention m2 WHERE m2.comment_message_id = m1.comment_message_id AND m2.user_id = :placeholder)",
           nativeQuery = true)
    void deleteRedundantMentions(@Param("userId") String userId, @Param("placeholder") String placeholder);

    // Safe to update in place: deleteRedundantMentions() already removed any row that would
    // collide with an existing placeholder mention on the same message.
    @Modifying
    @Transactional
    @Query(value = "UPDATE commenting.mention SET user_id = :placeholder WHERE user_id = :userId", nativeQuery = true)
    void anonymizeMentions(@Param("userId") String userId, @Param("placeholder") String placeholder);
}
