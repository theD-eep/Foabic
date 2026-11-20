package dev.foabic.api.mod;

import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

import java.util.List;
import java.util.Objects;

/**
 * Immutable metadata read from a mod's {@code foabic.mod} file.
 */
public record ModMetadata(
        String id,
        String name,
        Version version,
        String description,
        List<String> authors,
        List<String> entrypoints,
        List<Dependency> dependencies,
        List<Dependency> optionalDependencies,
        Environment environment,
        VersionRange apiRange
) {

    public ModMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        description = description == null ? "" : description;
        authors = List.copyOf(authors == null ? List.of() : authors);
        entrypoints = List.copyOf(entrypoints == null ? List.of() : entrypoints);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        optionalDependencies = List.copyOf(optionalDependencies == null ? List.of() : optionalDependencies);
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(apiRange, "apiRange");
        if (id.isBlank()) {
            throw new IllegalArgumentException("mod id must not be blank");
        }
        if (entrypoints.isEmpty()) {
            throw new IllegalArgumentException("mod " + id + " declares no entrypoint");
        }
    }

    public boolean supports(Environment runtime) {
        return environment == Environment.UNIVERSAL || environment == runtime;
    }
}
