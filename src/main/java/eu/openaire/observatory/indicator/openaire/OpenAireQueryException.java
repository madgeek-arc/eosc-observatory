package eu.openaire.observatory.indicator.openaire;

public class OpenAireQueryException extends RuntimeException {

    public OpenAireQueryException(String message) {
        super(message);
    }

    public OpenAireQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
