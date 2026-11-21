package dev.foabic.api.registry;

/** A registry operation failed, usually because the registry is frozen. */
public final class RegistryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RegistryException(String message) {
        super(message);
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
