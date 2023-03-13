package eu.eosc.observatory.websockets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "ws")
public class WebSocketProperties {
    String allowedOriginPattern;

    public String getAllowedOriginPattern() {
        return allowedOriginPattern;
    }

    public void setAllowedOriginPattern(String allowedOriginPattern) {
        this.allowedOriginPattern = allowedOriginPattern;
    }
}

