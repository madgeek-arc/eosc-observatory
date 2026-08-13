package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownIndicatorException extends RuntimeException {
    public UnknownIndicatorException(String code) {
        super("Unknown indicator: " + code);
    }
}
