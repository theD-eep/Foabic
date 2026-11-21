package dev.foabic.api.mod;

import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

import java.util.List;
import java.util.Objects;

/**
 * Parsed {@code foabic.mod} metadata.
 *
 * <p>{@link #texture()} is an optional, platform-neutral texture id such as
 * {@code mymod:block/generator}. When present, the loader generates the
 * blockstate, block model and item model for every block the mod registers
 * through {@link ModContext#submitBlock}. The referenced PNG is read from
 * {@code assets/<namespace>/textures/<path>.png} inside the mod jar.
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
        VersionRange apiRange,
        String texture
) {
    public ModMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
        description = description == null ? "" : description;
        authors = List.copyOf(authors == null ? List.of() : authors);
        entrypoints = List.copyOf(entrypoints == null ? List.of() : entrypoints);
        dependencies = List.copyOf(dependencies == null ? List.of() : dependencies);
        optionalDependencies = List.copyOf(
                optionalDependencies == null ? List.of() : optionalDependencies);
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(apiRange, "apiRange");
        texture = texture == null ? "" : texture.trim();
        if (id.isBlank()) throw new IllegalArgumentException("mod id must not be blank");
        if (entrypoints.isEmpty()) throw new IllegalArgumentException("mod " + id + " declares no entrypoint");
    }

    public boolean supports(Environment runtime) {
        return environment == Environment.UNIVERSAL || environment == runtime;
    }

    /** Whether this mod declared a simple generated texture. */
    public boolean hasTexture() {
        return !texture.isEmpty();
    }
}
