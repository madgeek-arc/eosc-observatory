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
import eu.openaire.observatory.commenting.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SurveyAnswerCommentService implements CommentService {

    private final CommentRepository repository;

    public SurveyAnswerCommentService(CommentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Comment> get(CommentTarget target) {
        return List.of();
    }

    @Override
    public List<Comment> get(CommentTarget target, CommentStatus status) {
        return repository.findAllByTargetAndStatus(target, status);
    }

    @Override
    public Comment add(Comment comment) {
        return repository.save(comment);
    }

    @Override
    public Comment update(UUID id, Comment comment) {
        return null;
    }

    @Override
    public Comment updateMessage(UUID id, CommentMessage message) {
        return null;
    }

    @Override
    public Comment reply(CommentMessage parentMessage, CommentMessage replyMessage) {
        return null;
    }

    @Override
    public Comment resolve(UUID id) {
        return null;
    }

    @Override
    public void delete(Comment comment) {

    }

    @Override
    public void delete(CommentMessage message) {

    }
}
