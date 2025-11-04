/**
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
package eu.openaire.observatory.commenting;


import eu.openaire.observatory.IntegrationTestConfig;
import eu.openaire.observatory.commenting.domain.Comment;
import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.domain.CommentType;
import eu.openaire.observatory.commenting.repository.CommentMessageRepository;
import eu.openaire.observatory.commenting.repository.CommentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Import({CommentingDatasourceConfig.class, SecurityAuditorAware.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentingTests extends IntegrationTestConfig {

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    CommentMessageRepository commentMessageRepository;

    static UUID getCommentId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    static Comment createComment() {
        Comment comment = new Comment(getCommentId());
        comment.setPath("this.is.a.test.path");
        comment.setStatus(CommentStatus.ACTIVE);
        comment.setType(CommentType.COMMENT);
        return comment;
    }

    @BeforeEach
    void addComments() {
        // insert comment with specific id and a message
        Comment comment = createComment();
        CommentMessage message = new CommentMessage();
        message.setBody("My comment message");
        comment.addMessage(message);
        commentRepository.save(comment);

        // insert empty comment with random id
        Comment comment2 = new Comment();
        comment2.setStatus(CommentStatus.ACTIVE);
        comment2.setType(CommentType.NOTE);
        commentRepository.save(comment2);
    }

    @AfterEach
    void delete() {
        commentRepository.deleteAll();
    }

    @Test
    void findComment() {
        Optional<Comment> comment = commentRepository.findById(getCommentId());
        assertTrue(comment.isPresent());
    }

    @Test
    void addMessage() {
        CommentMessage message = new CommentMessage();
        message.setBody("This is a test comment message");
        message.setComment(commentRepository.findById(getCommentId()).get());
        commentMessageRepository.save(message);

        Comment comment = commentRepository.findById(getCommentId()).orElseThrow();
        assertEquals(comment.getMessages().size(), 2);
    }

    @Test
    void addMessageReply() {
        Comment comment = commentRepository.findById(getCommentId()).orElseThrow();
        int size = comment.getMessages().size();

        CommentMessage message = comment.getMessages().getLast();
        CommentMessage reply = new CommentMessage();
        reply.setBody("This is a reply.");
        message.attachReply(reply);
        commentMessageRepository.save(reply);

        CommentMessage reloaded = commentMessageRepository.findById(reply.getId()).orElseThrow();
        assertEquals(reloaded.getParent().getId(), message.getId());
        assertEquals(reloaded.getComment().getId(), comment.getId());

        comment = commentRepository.findById(getCommentId()).orElseThrow();
        assertNotEquals(size, comment.getMessages().size());
    }

    @Test
    void editMessage() {
        Comment comment = commentRepository.findById(getCommentId()).orElseThrow();
        CommentMessage message = comment.getMessages().getLast();
        String body = "This is the correct message!";
        message.setBody(body);
        commentMessageRepository.save(message);
        comment = commentRepository.findById(getCommentId()).orElseThrow();
        assertEquals(comment.getMessages().getFirst().getBody(), body);
    }

    @Test
    void deleteMessage() {
        Comment comment = commentRepository.findById(getCommentId()).orElseThrow();
        int size = comment.getMessages().size();
        CommentMessage message = comment.getMessages().getLast();
        commentMessageRepository.deleteById(message.getId());
        comment = commentRepository.findById(getCommentId()).orElseThrow();
        assertNotEquals(comment.getMessages().size(), size);
    }

    @Test
    void deleteComment() {
        commentRepository.deleteById(getCommentId());
    }

}
