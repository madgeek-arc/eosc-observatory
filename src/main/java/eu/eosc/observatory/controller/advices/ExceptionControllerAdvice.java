package eu.eosc.observatory.controller.advices;

import gr.athenarc.catalogue.controller.GenericExceptionController;
import gr.athenarc.catalogue.exception.ServerError;
import io.sentry.Sentry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ExceptionControllerAdvice extends GenericExceptionController {

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ServerError> handleException(HttpServletRequest req, Exception ex) {
        // Forwards exception to Sentry
        Sentry.captureException(ex);
        return super.handleException(req, ex);
    }
}
