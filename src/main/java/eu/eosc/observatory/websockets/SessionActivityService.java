package eu.eosc.observatory.websockets;

import eu.eosc.observatory.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.*;

@Service
public class SessionActivityService {

    private static final Logger logger = LoggerFactory.getLogger(SessionActivityService.class);

    private final Map<String, Map<String, Set<SessionActivity>>> resourcesMap = Collections.synchronizedMap(new TreeMap<>());
    private final SimpMessagingTemplate simpMessagingTemplate;

    public SessionActivityService(SimpMessagingTemplate simpMessagingTemplate) {
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

    public Collection<SessionActivity> add(String sessionId, String type, String id, String action, Authentication auth) {
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

    public Collection<SessionActivity> delete(String sessionId, String type, String id, String action, Authentication auth) {
        if (resourcesMap.containsKey(type) && resourcesMap.get(type).containsKey(id)) {
            Set<SessionActivity> userActivities = resourcesMap.get(type).get(id);
            SessionActivity activity = new SessionActivity(sessionId, User.of(auth).getFullname(), action);
            userActivities.remove(activity);
        }
        return resourcesMap.containsKey(type) ? resourcesMap.get(type).get(id) : Set.of();
    }

    public Collection<SessionActivity> editSurvey(String sessionId, String type, String id, String field) {
        Set<SessionActivity> activities = resourcesMap.get(type).get(id);
        for (SessionActivity activity : activities) {
            if (activity.getSessionId().equals(sessionId)) {
                activity.setPosition(field);
            }
        }

        return resourcesMap.get(type).get(id);
    }
}
