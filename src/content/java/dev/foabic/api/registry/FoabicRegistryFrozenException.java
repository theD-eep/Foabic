package dev.foabic.api.registry;

/**
 * Thrown when content is submitted after the platform registration window has
 * closed. The adapter also logs the exact mod id and object id before throwing.
 */
public final class FoabicRegistryFrozenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String modId;
    private final String objectId;

    public FoabicRegistryFrozenException(String modId, String objectId, String message) {
        super(message);
        this.modId = modId;
        this.objectId = objectId;
    }

    public String modId() {
        return modId;
    }

    public String objectId() {
        return objectId;
    }
}
