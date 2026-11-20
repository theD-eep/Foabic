package dev.foabic.api.mod;

import java.nio.file.Path;
import java.util.List;

/**
 * A loaded mod. The container is created by the loader before the mod's
 * initializer runs.
 */
public interface ModContainer {

    String id();

    String name();

    dev.foabic.api.version.Version version();

    String description();

    List<String> authors();

    /**
     * The side this mod declared, which may be {@link Environment#UNIVERSAL}.
     */
    Environment environment();

    ModMetadata metadata();

    /**
     * The jar from which this mod was loaded.
     */
    Path jar();

    /**
     * A private, writable directory for this mod's data.
     */
    Path dataDirectory();

    System.Logger logger();

    ModResources resources();

    /**
     * A private properties-backed configuration file, loaded lazily.
     */
    dev.foabic.api.config.Config config();

    /**
     * The class loader that loaded this mod's classes.
     */
    ClassLoader classLoader();
}
