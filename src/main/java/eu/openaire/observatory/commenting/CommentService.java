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

import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CreateComment;
import eu.openaire.observatory.commenting.dto.CreateMessage;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    /**
     * Retrieves comment thread by id.
     *
     * @param id the comment id.
     * @return
     */
    CommentDto get(UUID id);

    /**
     * Retrieves all comment threads of the specified target.
     *
     * @param targetId the comment target id.
     * @return
     */
    List<CommentDto> get(String targetId);

    /**
     * Retrieves all comment threads of the specified target and status.
     *
     * @param targetId the comment target id.
     * @param status   the comment status
     * @return
     */
    List<CommentDto> get(String targetId, CommentStatus status);

    /**
     * Adds a comment thread.
     *
     * @param comment
     * @return
     */
    CommentDto add(CreateComment comment);

    /**
     * Updates a comment thread.
     *
     * @param id      the id of the comment to update
     * @param comment the comment to save
     * @return
     */
    CommentDto update(UUID id, CommentDto comment);

    /**
     * Adds a message to a comment thread.
     *
     * @param threadId the id of the comment thread
     * @param message  the new message
     * @return
     */
    CommentDto addMessage(UUID threadId, CreateMessage message);

    /**
     * Updates a comment message.
     *
     * @param messageId the id of the comment message to update
     * @param message   the updated message
     * @return
     */
    CommentDto updateMessage(UUID messageId, CreateMessage message);

    /**
     * Creates a reply to a comment message.
     *
     * @param replyToId    the message to reply to
     * @param replyMessage the reply message
     * @return
     */
    CommentDto reply(UUID replyToId, CreateMessage replyMessage);

    /**
     * Resolves a comment thread.
     *
     * @param id the comment id
     * @return
     */
    CommentDto resolve(UUID id);

    /**
     * Deletes a comment thread.
     *
     * @param commentId the id of the comment to delete
     */
    void delete(UUID commentId);

    /**
     * Deletes a comment message.
     *
     * @param messageId the id of the message to delete
     */
    void deleteMessage(UUID messageId);


}
