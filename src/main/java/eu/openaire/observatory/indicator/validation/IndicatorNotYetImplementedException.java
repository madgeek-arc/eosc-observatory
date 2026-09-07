package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown for an indicator that is registered in {@code IndicatorDefinitionCatalog} — so it appears
 * in the catalog and in presets — but whose query path is not implemented yet (typically because an
 * upstream data source it needs isn't wired). Extends {@link UnsupportedOperationException} so
 * {@code GenericExceptionController} renders it as HTTP 501 with the real message (its dedicated
 * {@code instanceof UnsupportedOperationException} branch, logged at INFO); a plain
 * {@link RuntimeException} would be masked as "Internal Server Error" and logged at ERROR.
 */
@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
public class IndicatorNotYetImplementedException extends UnsupportedOperationException {
    public IndicatorNotYetImplementedException(String message) {
        super(message);
    }
}
