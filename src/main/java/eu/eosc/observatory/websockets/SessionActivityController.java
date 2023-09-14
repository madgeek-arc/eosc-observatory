package eu.eosc.observatory.websockets;

import eu.eosc.observatory.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.*;

@Controller
public class SessionActivityController {

    private static final Logger logger = LoggerFactory.getLogger(SessionActivityController.class);

    private final Map<String, Map<String, Set<SessionActivity>>> resourcesMap = Collections.synchronizedMap(new TreeMap<>());
    private final SimpMessagingTemplate simpMessagingTemplate;

    public SessionActivityController(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        logger.debug(event.getSessionId());
        SessionActivity userSession = new SessionActivity(event.getSessionId());
        for (Map.Entry<String, Map<String, Set<SessionActivity>>> resourceEntry : resourcesMap.entrySet()) {
            for (Map.Entry<String, Set<SessionActivity>> idsEntries : resourceEntry.getValue().entrySet()) {
                if (idsEntries.getValue().remove(userSession)) {
                    simpMessagingTemplate.convertAndSend(String.format("/topic/active-users/%s/%s", resourceEntry.getKey(), idsEntries.getKey()), resourceEntry.getValue().get(idsEntries.getKey()));
                }
            }
        }
    }

    @MessageMapping("join/{type}/{id}")
    @SendTo("/topic/active-users/{type}/{id}")
    public Collection<SessionActivity> addUser(@Header("simpSessionId") String sessionId,
                                               @DestinationVariable("type") String type,
                                               @DestinationVariable("id") String id,
                                               String action, @Parameter(hidden = true) Authentication auth) {
        resourcesMap.putIfAbsent(type, new TreeMap<>());
        resourcesMap.get(type).putIfAbsent(id, new HashSet<>());
        try {
            String user = User.of(auth).getFullname();
            resourcesMap.get(type).get(id).add(new SessionActivity(sessionId, user, action));
        } catch (Exception e) {
            logger.error("user unauthorized");
        }
        logger.info("{}: {}", id, action);
        return resourcesMap.get(type).get(id);
    }

    @MessageMapping("leave/{type}/{id}")
    @SendTo("/topic/active-users/{type}/{id}")
    public Collection<SessionActivity> removeUser(@Header("simpSessionId") String sessionId,
                                                  @DestinationVariable("type") String type,
                                                  @DestinationVariable("id") String id,
                                                  String action, @Parameter(hidden = true) Authentication auth) {
        if (resourcesMap.containsKey(type) && resourcesMap.get(type).containsKey(id)) {
            Set<SessionActivity> userActivities = resourcesMap.get(type).get(id);
            SessionActivity activity = new SessionActivity(sessionId, User.of(auth).getFullname(), action);
            userActivities.remove(activity);
        }
        return resourcesMap.get(type).get(id);
    }
}
