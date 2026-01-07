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
package eu.openaire.observatory.controller;

import eu.openaire.observatory.commenting.CommentService;
import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CreateComment;
import eu.openaire.observatory.commenting.dto.CreateMessage;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "survey-answer-comments", produces = MediaType.APPLICATION_JSON_VALUE)
public class SurveyAnswerCommentController {

    private final CommentService commentService;

    public SurveyAnswerCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public CommentDto add(@RequestBody @Valid CreateComment comment) {
        return commentService.add(comment);
    }

    @PostMapping("{threadId}")
    public CommentDto addMessage(@PathVariable UUID threadId, @RequestBody @Valid CreateMessage comment) {
        return commentService.addMessage(threadId, comment);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("{threadId}")
    public CommentDto update(@PathVariable UUID threadId, @RequestBody CommentDto comment) {
        return commentService.update(threadId, comment);
    }

    @PutMapping("{threadId}/messages/{messageId}")
    public CommentDto updateMessage(@PathVariable UUID threadId, @PathVariable UUID messageId, @RequestBody @Valid CreateMessage message) throws AccessDeniedException {
        return commentService.updateMessage(messageId, message);
    }

    @GetMapping()
    public List<CommentDto> get(@RequestParam String targetId, @RequestParam CommentStatus status) {
        return commentService.get(targetId, status);
    }

    @PutMapping("{threadId}/resolve")
    public CommentDto resolve(@PathVariable UUID threadId) {
        return commentService.resolve(threadId);
    }

    @DeleteMapping("{threadId}")
    public void delete(@PathVariable UUID threadId) {
        commentService.delete(threadId);
    }

    @DeleteMapping("{threadId}/messages/{messageId}")
    public void deleteMessage(@PathVariable UUID threadId, @PathVariable UUID messageId) {
        commentService.deleteMessage(messageId);
    }
}
