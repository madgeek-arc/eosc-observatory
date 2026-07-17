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

package eu.openaire.observatory.service;

import eu.openaire.observatory.commenting.CommentNotificationService;
import eu.openaire.observatory.commenting.CommentService;
import eu.openaire.observatory.commenting.domain.CommentMessage;
import eu.openaire.observatory.commenting.domain.CommentStatus;
import eu.openaire.observatory.commenting.domain.CommentTarget;
import eu.openaire.observatory.commenting.domain.CommentThread;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CreateComment;
import eu.openaire.observatory.commenting.dto.CreateMessage;
import eu.openaire.observatory.commenting.repository.CommentMessageRepository;
import eu.openaire.observatory.commenting.repository.CommentRepository;
import eu.openaire.observatory.domain.User;
import eu.openaire.observatory.mappers.CommentMapper;
import org.hibernate.Hibernate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SurveyAnswerCommentService implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentMessageRepository messageRepository;
    private final CommentMapper mapper;
    private final CommentNotificationService notificationService;

    private static final String TARGET_TYPE = "survey_answer";

    // Mirrors the mention markup the frontend renders into a message body, "@{Display Name}(user-id)"
    // — CreateMessage/CommentMessage only carry the free-text body plus a separate structured
    // mentions list, so this format isn't defined anywhere else on the backend. If the frontend's
    // rendering convention ever changes, this pattern needs to change with it.
    private static final String MENTION_PATTERN_TEMPLATE = "@\\{[^}]*}\\(%s\\)";

    public SurveyAnswerCommentService(CommentRepository commentRepository,
                                      CommentMessageRepository messageRepository,
                                      CommentMapper mapper,
                                      CommentNotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
    }


    @Override
    public CommentDto get(UUID id) {
        return commentRepository.findById(id).map(mapper::toDto).orElseThrow();
    }

    @Override
    public List<CommentDto> get(String targetId) {
        CommentTarget target = new CommentTarget(TARGET_TYPE, targetId);
        return commentRepository.findAllByTargetOrderByCreatedAtAsc(target).stream().map(mapper::toDto).toList();
    }

    @Override
    public List<CommentDto> get(String targetId, CommentStatus status) {
        CommentTarget target = new CommentTarget(TARGET_TYPE, targetId);
        return commentRepository.findAllByTargetAndStatusOrderByCreatedAtAsc(target, status).stream().map(mapper::toDto).toList();
    }

    @Override
    public CommentDto add(CreateComment comment) {
        CommentThread c = new CommentThread();
        CommentTarget target = new CommentTarget(TARGET_TYPE, comment.targetId());
        c.setTarget(target);
        c.setFieldId(comment.fieldId());
        c.setStatus(CommentStatus.ACTIVE);
        CommentMessage message = new CommentMessage();
        message.setComment(c);
        message.setBody(comment.message().body());
        message.addMentions(comment.message().mentions());
        c.setMessages(List.of(message));
        CommentThread saved = commentRepository.save(c);
        notificationService.notifyMentions(saved.getMessages().getFirst());
        return mapper.toDto(saved);
    }

    @Override
    public CommentDto update(UUID id, CommentDto comment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CommentDto addMessage(UUID threadId, CreateMessage message) {
        CommentThread c = commentRepository.findById(threadId).orElseThrow();
        CommentMessage m = new CommentMessage();
        m.setBody(message.body());
        m.addMentions(message.mentions());
        c.addMessage(m);
        messageRepository.save(m);
        notificationService.notifyMentions(m);
        return mapper.toDto(c);
    }

    @Override
    @Transactional
    public CommentDto updateMessage(UUID messageId, CreateMessage message) {
        CommentMessage m = messageRepository.findWithCommentById(messageId).orElseThrow();
        String authorId = User.getId(SecurityContextHolder.getContext().getAuthentication());
        if (!authorId.equals(m.getAuthorId())) {
            throw new AccessDeniedException("You are not allowed to update this comment.");
        }
        m.setBody(message.body());
        m.addMentions(message.mentions());
        messageRepository.save(m);
        notificationService.notifyMentions(m);
        return mapper.toDto(m.getComment());
    }

    @Override
    public CommentDto reply(UUID replyToId, CreateMessage replyMessage) {
        // TODO: create method when replies are supported
        throw new UnsupportedOperationException();
    }

    @Override
    public CommentDto resolve(UUID id) {
        CommentThread thread = commentRepository.findById(id).orElseThrow();
        thread.setStatus(CommentStatus.RESOLVED);
        return mapper.toDto(commentRepository.save(thread));
    }

    @Override
    public void delete(UUID id) {
        CommentThread thread = commentRepository.findById(id).orElseThrow();
        String authorId = User.getId(SecurityContextHolder.getContext().getAuthentication());
        if (!thread.getMessages().isEmpty() && !authorId.equals(thread.getMessages().getFirst().getAuthorId())) {
            throw new AccessDeniedException("You are not allowed to delete this comment thread.");
        }
        thread.setStatus(CommentStatus.DELETED);
        commentRepository.save(thread);
    }

    @Transactional
    public void anonymizeUser(String userId, String placeholder) {
        // scrub the mentioned user's email out of message bodies before anonymizeMentions()
        // overwrites the structured mention rows this lookup depends on
        Pattern pattern = Pattern.compile(String.format(MENTION_PATTERN_TEMPLATE, Pattern.quote(userId)), Pattern.CASE_INSENSITIVE);
        for (CommentMessage message : messageRepository.findMessagesMentioning(userId)) {
            String updated = pattern.matcher(message.getBody()).replaceAll(Matcher.quoteReplacement(placeholder));
            if (!updated.equals(message.getBody())) {
                message.setBody(updated);
                messageRepository.save(message);
            }
        }
        messageRepository.anonymizeAuthor(userId, placeholder);
        // Order matters: drop mention rows that would collide with an existing placeholder
        // mention on the same message before rewriting the remaining ones to the placeholder.
        messageRepository.deleteRedundantMentions(userId, placeholder);
        messageRepository.anonymizeMentions(userId, placeholder);
    }

    @Override
    public void deleteMessage(UUID messageId) {
        CommentMessage m = messageRepository.findById(messageId).orElseThrow();
        String authorId = User.getId(SecurityContextHolder.getContext().getAuthentication());
        if (!authorId.equals(m.getAuthorId())) {
            throw new AccessDeniedException("You are not allowed to delete this comment.");
        }
        messageRepository.delete(m);
    }
}
