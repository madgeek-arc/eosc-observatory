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
package eu.openaire.observatory.service;

import eu.openaire.observatory.commenting.CommentService;
import eu.openaire.observatory.commenting.domain.Comment;
import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.domain.CommentTarget;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CreateComment;
import eu.openaire.observatory.commenting.dto.CreateMessage;
import eu.openaire.observatory.commenting.repository.CommentRepository;
import eu.openaire.observatory.mappers.CommentMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SurveyAnswerCommentService implements CommentService {

    private final CommentRepository repository;
    private final CommentMapper mapper;

    private static final String TARGET_TYPE = "survey_answer";

    public SurveyAnswerCommentService(CommentRepository repository,
                                      CommentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<CommentDto> get(String targetId) {
        return List.of();
    }

    @Override
    public List<CommentDto> get(String targetId, CommentStatus status) {
        CommentTarget target = new CommentTarget(TARGET_TYPE, targetId);
        return repository.findAllByTargetAndStatus(target, status).stream().map(mapper::toDto).toList();
    }

    @Override
    public CommentDto add(CreateComment comment) {
        Comment c = mapper.toComment(comment);
        return mapper.toDto(repository.save(c));
    }

    @Override
    public CommentDto update(UUID id, CommentDto comment) {
        return null;
    }

    @Override
    public CommentDto updateMessage(UUID id, CreateMessage message) {
        return null;
    }

    @Override
    public CommentDto reply(UUID replyToId, CreateMessage replyMessage) {
        return null;
    }

    @Override
    public CommentDto resolve(UUID id) {
        return null;
    }

    @Override
    public void delete(UUID commentId) {

    }

    @Override
    public void deleteMessage(UUID messageId) {

    }
}
