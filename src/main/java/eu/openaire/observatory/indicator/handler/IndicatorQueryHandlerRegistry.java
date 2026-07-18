package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class IndicatorQueryHandlerRegistry {

    private final Map<String, IndicatorQueryHandler> handlers;

    public IndicatorQueryHandlerRegistry(List<IndicatorQueryHandler> handlers) {
        this.handlers = handlers.stream()
            .collect(Collectors.toUnmodifiableMap(
                IndicatorQueryHandler::handlerKey,
                Function.identity()
            ));
    }

    public IndicatorQueryHandler getRequired(String handlerKey) {
        var handler = handlers.get(handlerKey);
        if (handler == null) {
            throw new UnknownIndicatorException(handlerKey);
        }
        return handler;
    }
}
