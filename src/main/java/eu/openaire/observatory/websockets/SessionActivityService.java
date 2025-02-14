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

import eu.openaire.observatory.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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

    @Scheduled(fixedRate = 300_000)
    void removeInactiveSessions() {
        Date now = new Date();
        for (Map.Entry<String, Map<String, Set<SessionActivity>>> type : resourcesMap.entrySet()) {
            for (Map.Entry<String, Set<SessionActivity>> id : type.getValue().entrySet()) {
                boolean foundStale = false;
                for (Iterator<SessionActivity> it = id.getValue().iterator(); it.hasNext();) {
                    SessionActivity session = it.next();
                    if ((session.getDate().getTime() + 3_600_000) < now.getTime()) {
                        it.remove();
                        logger.debug("Removing stale session: {}", session);
                        foundStale = true;
                    }
                }
                if (foundStale) { // broadcast changes in active-users
                    simpMessagingTemplate.convertAndSend(String.format("/topic/active-users/%s/%s", type.getKey(), id.getKey()), id.getValue());
                }
            }
        }
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
            SessionActivity session = new SessionActivity(sessionId, user, action);
            resourcesMap.get(type).get(id).add(session);
        } catch (Exception e) {
            logger.error("User Unauthorized");
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

    public Collection<SessionActivity> focusOnField(String sessionId, String type, String id, String field, Authentication auth) {
        add(sessionId, type, id, "edit", auth);
        Set<SessionActivity> activities = resourcesMap.get(type).get(id);
        for (SessionActivity activity : activities) {
            if (activity.getSessionId().equals(sessionId)) {
                activity.setPosition(field);
                activity.setDate(new Date());
            }
        }

        return resourcesMap.get(type).get(id);
    }
}
