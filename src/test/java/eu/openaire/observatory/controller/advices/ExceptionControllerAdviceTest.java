package eu.openaire.observatory.controller.advices;

import gr.uoa.di.madgik.catalogue.exception.ServerError;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionControllerAdviceTest {

    private final ExceptionControllerAdvice advice = new ExceptionControllerAdvice();

    @Test
    void missingParameter_isBadRequestWithParameterName() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/answers/info");

        ResponseEntity<ServerError> response = advice.handleMissingParameter(request,
                new MissingServletRequestParameterException("groupId", "String"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("groupId is missing", response.getBody().getMessage());
    }
}
