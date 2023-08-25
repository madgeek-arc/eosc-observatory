package eu.eosc.observatory.sse;


import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

@Repository
public class InMemoryEmitterRepository implements EmitterRepository {

    private final Map<String, Emitter> userEmitters = new TreeMap<>();


    public InMemoryEmitterRepository() {
    }

    @Override
    public Emitter save(Emitter emitter) {
        return this.userEmitters.put(emitter.getUserId(), emitter);
    }

    @Override
    public Emitter get(String userId) {
        return this.userEmitters.get(userId);
    }

    @Override
    public Collection<Emitter> getAll() {
        return userEmitters.values();
    }

    @Override
    public void delete(String userId) {
        userEmitters.remove(userId).getSseEmitters().forEach(ResponseBodyEmitter::complete);
    }
}
