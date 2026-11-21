package dev.foabic.loader;

public class FoabicException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FoabicException(String message) {
        super(message);
    }

    public FoabicException(String message, Throwable cause) {
        super(message, cause);
    }
}
