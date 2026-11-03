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

import eu.openaire.observatory.service.UserPseudonymizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final UserPseudonymizer userPseudonymizer;

    public WebSocketEventListener(UserPseudonymizer userPseudonymizer) {
        this.userPseudonymizer = userPseudonymizer;
    }

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Websocket session [{}] connected by [{}]",
                    SimpMessageHeaderAccessor.getSessionId(event.getMessage().getHeaders()), pseudonym(event.getUser()));
        }
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("Websocket session [{}] disconnected ({}) by [{}]",
                    event.getSessionId(), event.getCloseStatus(), pseudonym(event.getUser()));
        }
    }

    private String pseudonym(Principal principal) {
        return principal instanceof Authentication auth ? userPseudonymizer.forLogging(auth) : "anonymous";
    }
}
