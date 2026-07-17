package eu.openaire.observatory.indicator.handler;

import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IndicatorQueryHandlerRegistry {

    private final Map<String, IndicatorQueryHandler> handlers;

    public IndicatorQueryHandlerRegistry(List<IndicatorQueryHandler> handlers) {
        this.handlers = handlers.stream()
            .collect(Collectors.toUnmodifiableMap(
                IndicatorQueryHandler::indicatorCode,
                Function.identity()
            ));
    }

    public IndicatorQueryHandler getRequired(String code) {
        var handler = handlers.get(code);
        if (handler == null) {
            throw new UnknownIndicatorException(code);
        }
        return handler;
    }
}
