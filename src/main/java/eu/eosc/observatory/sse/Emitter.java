package eu.eosc.observatory.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Emitter {

    String userId;
    String sessionId;
    List<SseEmitter> sseEmitters = Collections.synchronizedList(new ArrayList<>());

    public Emitter() {
    }

    public Emitter(String userId, String sessionId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<SseEmitter> getSseEmitters() {
        return sseEmitters;
    }

    public void addSseEmitter(SseEmitter sseEmitter) {
        this.sseEmitters.add(sseEmitter);
        sseEmitter.onCompletion(() -> sseEmitters.remove(sseEmitter));
    }

    public void setSseEmitters(List<SseEmitter> sseEmitters) {
        this.sseEmitters = sseEmitters;
    }
}
