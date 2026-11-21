package dev.foabic.api.mod;

import dev.foabic.api.version.Version;

import java.nio.file.Path;
import java.util.List;

/** Runtime metadata and classloader for one loaded Foabic mod. */
public interface ModContainer {

    String id();

    String name();

    Version version();

    String description();

    List<String> authors();

    Environment environment();

    ModMetadata metadata();

    Path jar();

    Path dataDirectory();

    System.Logger logger();

    ClassLoader classLoader();
}
