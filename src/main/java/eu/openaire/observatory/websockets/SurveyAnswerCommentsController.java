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
package eu.openaire.observatory.websockets;

import eu.openaire.observatory.commenting.CommentService;
import eu.openaire.observatory.commenting.dto.CommentDto;
import eu.openaire.observatory.commenting.dto.CreateComment;
import eu.openaire.observatory.commenting.dto.CreateMessage;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class SurveyAnswerCommentsController {

    private static final Logger logger = LoggerFactory.getLogger(SurveyAnswerCommentsController.class);
    private static final String BASE_PATH = "comments/survey_answer/{targetId}";
    private static final String BASE_TOPIC = "/topic/comments/survey_answer/{targetId}";

    private final CommentService commentService;

    public SurveyAnswerCommentsController(CommentService commentService) {
        this.commentService = commentService;
    }

    @MessageMapping(BASE_PATH)
    @SendTo(BASE_TOPIC)
    public CommentDto add(@DestinationVariable("targetId") String targetId,
                          @Valid CreateComment comment) {
        return commentService.add(comment);
    }

    @MessageMapping(BASE_PATH + "/{threadId}/messages")
    @SendTo(BASE_TOPIC)
    public CommentDto addMessage(@DestinationVariable("targetId") String targetId,
                                 @DestinationVariable("threadId") UUID threadId,
                                 @Valid CreateMessage message) {
        return commentService.addMessage(threadId, message);
    }

    @MessageMapping(BASE_PATH + "/{threadId}")
    @SendTo(BASE_TOPIC)
    public CommentDto update(@DestinationVariable("targetId") String targetId,
                             @DestinationVariable("threadId") UUID threadId,
                             CommentDto comment) {
        return commentService.update(threadId, comment);
    }

    @MessageMapping(BASE_PATH + "/{threadId}/messages/{messageId}")
    @SendTo(BASE_TOPIC)
    public CommentDto updateMessage(@DestinationVariable("targetId") String targetId,
                                    @DestinationVariable("threadId") UUID threadId,
                                    @DestinationVariable("messageId") UUID messageId,
                                    @Valid CreateMessage message) {
        return commentService.updateMessage(messageId, message);
    }

    @MessageMapping(BASE_PATH + "/{threadId}/resolve")
    @SendTo(BASE_TOPIC)
    public CommentDto resolve(@DestinationVariable("targetId") String targetId,
                              @DestinationVariable("threadId") UUID threadId) {
        return commentService.resolve(threadId);
    }

    @MessageMapping(BASE_PATH + "/{threadId}/messages/{messageId}/delete")
    @SendTo(BASE_TOPIC)
    public CommentDto deleteMessage(@DestinationVariable("targetId") String targetId,
                                    @DestinationVariable("threadId") UUID threadId,
                                    @DestinationVariable("messageId") UUID messageId) {
        commentService.deleteMessage(messageId);
        return commentService.get(threadId);
    }

    @MessageMapping(BASE_PATH + "/{threadId}/delete")
    @SendTo(BASE_TOPIC)
    public void delete(@DestinationVariable("targetId") String targetId,
                       @DestinationVariable("threadId") UUID threadId) {
    }
}
