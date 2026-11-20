package dev.foabic.loader;

/**
 * A mod could not be discovered, resolved or initialized.
 */
public final class ModLoadException extends FoabicException {

    private static final long serialVersionUID = 1L;

    public ModLoadException(String message) {
        super(message);
    }

    public ModLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
