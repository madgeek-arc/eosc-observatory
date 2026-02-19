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

import eu.openaire.observatory.domain.Revision;
import eu.openaire.observatory.service.SurveyService;
import io.swagger.v3.oas.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class ConcurrentEditController {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentEditController.class);

    private final SurveyService surveyService;

    public ConcurrentEditController(SurveyService surveyService) {
        this.surveyService = surveyService;
    }

    @MessageMapping("edit/{type}/{id}")
    @SendTo("/topic/edit/{type}/{id}")
    public Revision editField(@Header("simpSessionId") String sessionId,
                              @DestinationVariable("type") String type,
                              @DestinationVariable("id") String id,
                              Revision revision, @Parameter(hidden = true) Authentication auth) {
        revision.setSessionId(sessionId);
        surveyService.edit(id, revision, auth);
        return revision;
    }
}
