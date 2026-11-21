package dev.foabic.loader;

/**
 * A declared dependency is missing or has an incompatible version.
 *
 * <p>The loader resolves the whole dependency graph before loading any mod
 * entrypoint. This exception therefore replaces the old late
 * {@code NoClassDefFoundError} with one actionable message.
 */
public final class DependencyException extends ModLoadException {

    private static final long serialVersionUID = 1L;

    public DependencyException(String message) {
        super(message);
    }
}
