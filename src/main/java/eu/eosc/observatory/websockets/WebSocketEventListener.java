package eu.eosc.observatory.websockets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        logger.debug("{}", event);
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {
        logger.debug("{}", event);
    }
}
