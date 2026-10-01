package eu.openaire.observatory.indicator.openaire;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;

/**
 * A failure talking to the OpenAIRE stats-tool: an error status, an unusable response, or an unreachable
 * endpoint. Extends {@link ResourceException} so the real message reaches the client, with 502 Bad
 * Gateway unless a different status is given.
 */
public class OpenAireQueryException extends ResourceException {

    public OpenAireQueryException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }

    public OpenAireQueryException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_GATEWAY, cause);
    }

    public OpenAireQueryException(String message, HttpStatus status, Throwable cause) {
        super(message, status);
        initCause(cause);
    }
}
