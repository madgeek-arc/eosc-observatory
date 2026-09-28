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

package eu.openaire.observatory.websockets;

import eu.openaire.observatory.domain.Revision;
import eu.openaire.observatory.permissions.Permissions;
import eu.openaire.observatory.service.SecurityService;
import eu.openaire.observatory.service.SurveyService;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ConcurrentEditController {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentEditController.class);
    private static final String EDIT_TOPIC = "/topic/edit/{type}/{id}";

    private final SurveyService surveyService;
    private final SecurityService securityService;

    public ConcurrentEditController(SurveyService surveyService, SecurityService securityService) {
        this.surveyService = surveyService;
        this.securityService = securityService;
    }

    @MessageMapping("edit/{type}/{id}")
    @SendTo(EDIT_TOPIC)
    public Revision editField(@Header("simpSessionId") String sessionId,
                              @DestinationVariable("type") String type,
                              @DestinationVariable("id") String id,
                              Revision revision, @Parameter(hidden = true) Authentication auth) {
        if (!securityService.hasPermission(auth, Permissions.WRITE.getKey(), id)) {
            throw new AccessDeniedException("You are not allowed to edit this survey answer.");
        }
        revision.setSessionId(sessionId);
        surveyService.edit(id, revision, auth);
        return revision;
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser(EDIT_TOPIC)
    public String handleException(Exception e) {
        return e.getMessage();
    }
}
