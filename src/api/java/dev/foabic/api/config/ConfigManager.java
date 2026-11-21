package dev.foabic.api.config;

/**
 * Loads and saves small JSON configuration objects.
 *
 * <p>Records and ordinary Java objects with fields are supported. The first
 * call creates the file with defaults; later starts read it back. Files live
 * under {@code config/foabic/<mod-id>/<Type>.json}.
 */
public interface ConfigManager {

    /**
     * Returns the loaded config instance, creating the JSON file with defaults
     * on first use. Repeated calls for the same type return the same object.
     */
    <T> T register(Class<T> type);

    /** Writes the current values of a config instance back to its JSON file. */
    <T> void save(T config);
}
