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
package eu.openaire.observatory.websockets;

import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Collection;

@Controller
public class SessionActivityController {

    private static final Logger logger = LoggerFactory.getLogger(SessionActivityController.class);

    private final SessionActivityService sessionActivityService;

    public SessionActivityController(SessionActivityService sessionActivityService) {
        this.sessionActivityService = sessionActivityService;
    }

    @MessageMapping("join/{type}/{id}")
    @SendTo("/topic/active-users/{type}/{id}")
    public Collection<SessionActivity> addUser(@Header("simpSessionId") String sessionId,
                                               @DestinationVariable("type") String type,
                                               @DestinationVariable("id") String id,
                                               String action, @Parameter(hidden = true) Authentication auth) {
        return sessionActivityService.add(sessionId, type, id, action, auth);
    }

    @MessageMapping("leave/{type}/{id}")
    @SendTo("/topic/active-users/{type}/{id}")
    public Collection<SessionActivity> removeUser(@Header("simpSessionId") String sessionId,
                                                  @DestinationVariable("type") String type,
                                                  @DestinationVariable("id") String id,
                                                  String action, @Parameter(hidden = true) Authentication auth) {
        return sessionActivityService.delete(sessionId, type, id, action, auth);
    }

    @MessageMapping("focus/{type}/{id}/{field}")
    @SendTo("/topic/active-users/{type}/{id}")
    public Collection<SessionActivity> focusOnField(@Header("simpSessionId") String sessionId,
                                                  @DestinationVariable("type") String type,
                                                  @DestinationVariable("id") String id,
                                                  @DestinationVariable("field") String field,
                                                  @Parameter(hidden = true) Authentication auth) {
        return sessionActivityService.focusOnField(sessionId, type, id, field, auth);
    }
}
