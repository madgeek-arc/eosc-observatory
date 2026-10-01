package eu.openaire.observatory.indicator.survey;

import gr.uoa.di.madgik.registry.exception.ResourceException;
import org.springframework.http.HttpStatus;

/**
 * A failure talking to the survey stats-tool: an error status, an unusable response, or an unreachable
 * endpoint. Extends {@link ResourceException} so the real message reaches the client, with 502 Bad
 * Gateway unless a different status is given.
 */
public class SurveyStatsToolQueryException extends ResourceException {

    public SurveyStatsToolQueryException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }

    public SurveyStatsToolQueryException(String message, Throwable cause) {
        this(message, HttpStatus.BAD_GATEWAY, cause);
    }

    public SurveyStatsToolQueryException(String message, HttpStatus status, Throwable cause) {
        super(message, status);
        initCause(cause);
    }
}
