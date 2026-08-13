package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownDimensionException extends RuntimeException {
    public UnknownDimensionException(String code) {
        super("No dimension registered for code: " + code);
    }
}
