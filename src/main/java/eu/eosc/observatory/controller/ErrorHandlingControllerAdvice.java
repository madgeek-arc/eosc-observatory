package eu.eosc.observatory.controller;

import eu.openminted.registry.core.exception.ServerError;
import gr.athenarc.catalogue.exception.ResourceAlreadyExistsException;
import gr.athenarc.catalogue.exception.ResourceNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ErrorHandlingControllerAdvice {

    private static final Logger logger = LogManager.getLogger(ErrorHandlingControllerAdvice.class);

    // 401
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseBody
    ServerError handleUnauthorized(HttpServletRequest req, Exception ex) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        return new ServerError(req.getRequestURL().toString(),ex);
    }

    // 403
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    ServerError handleAccessDenied(HttpServletRequest req, Exception ex) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        return new ServerError(req.getRequestURL().toString(),ex);
    }

    // 404
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseBody
    ServerError handleNotFound(HttpServletRequest req, Exception ex) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        return new ServerError(req.getRequestURL().toString(),ex);
    }

    // 409
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    @ResponseBody
    ServerError handleAlreadyExists(HttpServletRequest req, Exception ex) {
        logger.info(ex.getMessage());
        logger.debug(ex);
        return new ServerError(req.getRequestURL().toString(),ex);
    }
}
