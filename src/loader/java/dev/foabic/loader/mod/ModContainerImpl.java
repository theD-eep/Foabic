package dev.foabic.loader.mod;

import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.mod.ModMetadata;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ModContainerImpl implements ModContainer {

    private final ModMetadata metadata;
    private final Path jar;
    private final Path dataDirectory;
    private final ClassLoader classLoader;
    private final System.Logger logger;

    public ModContainerImpl(ModMetadata metadata, Path jar, Path dataDirectory,
                            ClassLoader classLoader) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.jar = Objects.requireNonNull(jar, "jar");
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
        this.logger = System.getLogger("Foabic/" + metadata.id());
    }

    @Override public String id() { return metadata.id(); }
    @Override public String name() { return metadata.name(); }
    @Override public String version() { return metadata.version(); }
    @Override public String description() { return metadata.description(); }
    @Override public List<String> authors() { return metadata.authors(); }
    public Environment environment() { return metadata.environment(); }
    public ModMetadata metadata() { return metadata; }
    @Override public Path jar() { return jar; }
    @Override public Path dataDirectory() { return dataDirectory; }
    @Override public System.Logger logger() { return logger; }
    @Override public ClassLoader classLoader() { return classLoader; }
}
