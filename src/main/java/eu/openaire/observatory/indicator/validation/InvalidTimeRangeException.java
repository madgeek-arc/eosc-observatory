package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTimeRangeException extends RuntimeException {
    public InvalidTimeRangeException(Integer from, Integer to, int currentYear) {
        super("Invalid time range: from=" + from + ", to=" + to
            + " (must satisfy 2015 <= from <= to <= " + currentYear + ")");
    }
}
