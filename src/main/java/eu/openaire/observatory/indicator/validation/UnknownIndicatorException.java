package eu.openaire.observatory.indicator.validation;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownIndicatorException extends ResourceException {
    public UnknownIndicatorException(String code) {
        super("Unknown indicator: " + code, HttpStatus.NOT_FOUND);
    }
}
