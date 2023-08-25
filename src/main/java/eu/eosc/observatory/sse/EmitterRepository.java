package eu.eosc.observatory.sse;

import java.util.Collection;


public interface EmitterRepository {

    Emitter save(Emitter emitter);

    Emitter get(String userId);

    Collection<Emitter> getAll();

    void delete(String userId);

}
