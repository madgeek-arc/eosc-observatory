package eu.openaire.observatory.indicator.dimension;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownDimensionException extends ResourceException {
    public UnknownDimensionException(String dimensionCode) {
        super("No member provider registered for dimension " + dimensionCode, HttpStatus.NOT_FOUND);
    }
}
