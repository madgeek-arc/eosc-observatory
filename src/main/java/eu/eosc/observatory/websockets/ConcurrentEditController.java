package eu.eosc.observatory.websockets;

import eu.eosc.observatory.domain.Revision;
import eu.eosc.observatory.service.SurveyService;
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
