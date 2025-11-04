/*
 * Copyright 2021-2025 OpenAIRE AMKE
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.openaire.observatory.commenting;

import eu.openaire.observatory.commenting.domain.Comment;
import eu.openaire.observatory.commenting.domain.CommentMessage;

import java.util.UUID;

public interface CommentService {

    /**
     * Adds a comment thread.
     *
     * @param comment
     * @return
     */
    Comment add(Comment comment);

    /**
     * Updates a comment thread.
     *
     * @param id      the id of the comment to update
     * @param comment the comment to save
     * @return
     */
    Comment update(UUID id, Comment comment);

    /**
     * Updates a comment message.
     *
     * @param id      the id of the comment message to update
     * @param message the new message
     * @return
     */
    Comment updateMessage(UUID id, CommentMessage message);

    /**
     * Creates a reply to a comment message.
     *
     * @param parentMessage the message to reply to
     * @param replyMessage  the reply message
     * @return
     */
    Comment reply(CommentMessage parentMessage, CommentMessage replyMessage);

    /**
     * Resolves a comment thread.
     *
     * @param id the comment id
     * @return
     */
    Comment resolve(UUID id);

    /**
     * Deletes a comment thread.
     *
     * @param comment the comment to delete
     */
    void delete(Comment comment);

    /**
     * Deletes a comment message.
     *
     * @param message the message to delete
     */
    void delete(CommentMessage message);


}
