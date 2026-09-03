package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class IndicatorAccessDeniedException extends AccessDeniedException {
    public IndicatorAccessDeniedException(String code) {
        super("Access denied for indicator: " + code);
    }
}
