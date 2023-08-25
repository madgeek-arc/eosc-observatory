package eu.eosc.observatory.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class EmitterService {

    private final EmitterRepository emitterRepository;
    private final ExecutorService sseExecutorService = Executors.newWorkStealingPool();

    public EmitterService(EmitterRepository emitterRepository) {
        this.emitterRepository = emitterRepository;
    }

    public Emitter getUserEmitter(String userId) {
        Emitter emitter = emitterRepository.get(userId);
        if (emitter == null) {
            emitter = new Emitter();
            emitter.setUserId(userId);
            emitterRepository.save(emitter);
        }
        return emitter;
    }

    public void sendDataToUserEmitters(String userId, EmitterMessage message) {
        sendDataToEmitter(emitterRepository.get(userId), message);
    }

    private void sendDataToEmitter(Emitter emitter, EmitterMessage message) {
        if (emitter != null) {
            for (SseEmitter sseEmitter : emitter.getSseEmitters()) {
                sseExecutorService.execute(() -> {
                    try {
                        SseEmitter.SseEventBuilder event = SseEmitter.event()
                                .id(message.getId())
                                .name(message.getEventName())
                                .data(message.getData())
                                .reconnectTime(1000)
                                .comment(message.getComment());
                        sseEmitter.send(event);
                    } catch (Exception ex) {
                        sseEmitter.completeWithError(ex);
                        emitter.getSseEmitters().remove(sseEmitter);
//                    emitterRepository.deleteByUser(emitter.getUserId());
                    }
                });
            }
        }
    }
}
