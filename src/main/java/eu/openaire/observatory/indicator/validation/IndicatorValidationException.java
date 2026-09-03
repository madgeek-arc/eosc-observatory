package eu.openaire.observatory.indicator.validation;

import gr.uoa.di.madgik.catalogue.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base for indicator request-validation failures. Extends the catalogue {@link ValidationException}
 * so {@code GenericExceptionController} renders it as HTTP 400 with the real message — a plain
 * {@link RuntimeException} is masked as "Internal Server Error" and logged at ERROR level.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IndicatorValidationException extends ValidationException {
    public IndicatorValidationException(String message) {
        super(message);
    }
}
