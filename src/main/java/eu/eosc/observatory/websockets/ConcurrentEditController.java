package eu.eosc.observatory.websockets;

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

    @MessageMapping("revision/{type}/{id}")
    @SendTo("/topic/revision/{type}/{id}")
    public String editField(@Header("simpSessionId") String sessionId,
                            @DestinationVariable("type") String type,
                            @DestinationVariable("id") String id,
                            String value, @Parameter(hidden = true) Authentication auth) {
        return value;
    }
}
