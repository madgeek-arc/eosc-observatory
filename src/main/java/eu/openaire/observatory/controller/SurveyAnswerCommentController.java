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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openaire.observatory.commenting.CommentService;
import eu.openaire.observatory.commenting.domain.Comment;
import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.domain.CommentTarget;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("survey-answer-comment")
public class SurveyAnswerCommentController {

    private final CommentService commentService;
    private final ObjectMapper objectMapper;

    public SurveyAnswerCommentController(CommentService commentService,
                                         ObjectMapper objectMapper) {
        this.commentService = commentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Comment add(@RequestBody JsonNode comment) throws JsonProcessingException {
        Comment c = objectMapper.readValue(comment.toPrettyString(), Comment.class);
        return commentService.add(c);
    }

    @GetMapping()
    public List<Comment> get(CommentTarget target,  CommentStatus status) {
        return commentService.get(target, status);
    }
}
