package eu.openaire.observatory.indicator.predefined;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PresetFilterConflictException extends RuntimeException {
    public PresetFilterConflictException(String widgetId, String dimensionCode) {
        super("Widget '" + widgetId + "' already presets a filter on '" + dimensionCode
            + "' — the request must not filter that dimension itself");
    }
}
