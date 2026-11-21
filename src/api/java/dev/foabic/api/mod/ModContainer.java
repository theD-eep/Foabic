package dev.foabic.api.mod;

import java.nio.file.Path;
import java.util.List;

/** Runtime metadata and classloader for one loaded Foabic mod. */
public interface ModContainer {

    String id();

    String name();

    /** The declared version string, as written in {@code foabic.mod.json}. */
    String version();

    String description();

    List<String> authors();

    Path jar();

    Path dataDirectory();

    System.Logger logger();

    ClassLoader classLoader();
}
