package eu.openaire.observatory.indicator.validation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnsupportedDimensionException extends RuntimeException {
    public UnsupportedDimensionException(String indicatorCode, String dimensionCode) {
        super("Dimension " + dimensionCode + " not supported by indicator " + indicatorCode);
    }
}
