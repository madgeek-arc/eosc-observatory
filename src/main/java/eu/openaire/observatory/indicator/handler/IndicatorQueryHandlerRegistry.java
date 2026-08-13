package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.KeyedRegistry;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndicatorQueryHandlerRegistry {

    private final KeyedRegistry<String, IndicatorQueryHandler> handlers;

    public IndicatorQueryHandlerRegistry(List<IndicatorQueryHandler> handlers) {
        this.handlers = new KeyedRegistry<>(handlers, IndicatorQueryHandler::handlerKey);
    }

    public IndicatorQueryHandler getRequired(String handlerKey) {
        return handlers.getRequired(handlerKey, UnknownIndicatorException::new);
    }
}
