package eu.openaire.observatory.controller.advices;

import eu.openaire.observatory.indicator.validation.IndicatorAccessDeniedException;
import eu.openaire.observatory.indicator.validation.MissingRequiredFilterException;
import eu.openaire.observatory.indicator.validation.UnknownIndicatorException;
import gr.uoa.di.madgik.catalogue.exception.ServerError;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionControllerAdviceTest {

    private final ExceptionControllerAdvice advice = new ExceptionControllerAdvice();
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void unknownIndicatorExceptionMapsToNotFound() {
        when(request.getRequestURI()).thenReturn("/indicator-queries");

        ResponseEntity<ServerError> response = advice.handleException(request, new UnknownIndicatorException("does.not.exist"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void indicatorAccessDeniedExceptionMapsToForbidden() {
        when(request.getRequestURI()).thenReturn("/indicator-queries");

        ResponseEntity<ServerError> response = advice.handleException(request, new IndicatorAccessDeniedException("publications.count"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void missingRequiredFilterExceptionMapsToBadRequest() {
        when(request.getRequestURI()).thenReturn("/indicator-queries");

        ResponseEntity<ServerError> response = advice.handleException(request, new MissingRequiredFilterException("publications.count", "country"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void unannotatedExceptionStillFallsBackToInternalServerError() {
        when(request.getRequestURI()).thenReturn("/indicator-queries");

        ResponseEntity<ServerError> response = advice.handleException(request, new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
