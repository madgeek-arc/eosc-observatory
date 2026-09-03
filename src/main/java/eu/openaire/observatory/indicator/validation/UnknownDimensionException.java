package eu.openaire.observatory.indicator.validation;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownDimensionException extends ResourceException {
    public UnknownDimensionException(String code) {
        super("No dimension registered for code: " + code, HttpStatus.NOT_FOUND);
    }
}
