package dev.foabic.api.mod;

import java.util.List;
import java.util.Objects;

/**
 * Parsed {@code foabic.mod.json} metadata.
 *
 * <p>Versions and dependency ranges are strings so the core artifact does not
 * carry a version parser. The loader validates and evaluates them before any
 * mod class is loaded.
 *
 * <p>{@link #texture()} is an optional platform-neutral texture id such as
 * {@code mymod:block/generator}. When present, Foabic generates the blockstate,
 * block model and item model for every block registered through
 * {@link ModContext#submitBlock}.
 *
 * <p>{@link #icon()} is an optional PNG path inside the mod jar, normally
 * {@code icon.png} or {@code assets/mymod/icon.png}. The Foabic Loom plugin
 * translates it into each platform's metadata.
 *
 * <p>{@link #dataProviders()} lists public no-argument classes implementing
 * {@link dev.foabic.api.data.FoabicDataProvider}. They run in the build-time
 * {@code foabicData} task and again in the loader, producing standard recipe,
 * tag, loot-table, model and language JSON.
 */
public record ModMetadata(
        String id,
        String name,
        String version,
        String description,
        List<String> authors,
        List<String> entrypoints,
        List<Dependency> dependencies,
        List<Dependency> optionalDependencies,
        Environment environment,
        String apiRange,
        String texture,
        String icon,
        List<String> dataProviders
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
        apiRange = apiRange == null || apiRange.isBlank() ? "*" : apiRange.trim();
        texture = texture == null ? "" : texture.trim();
        icon = icon == null ? "" : icon.trim().replace('\\', '/');
        dataProviders = List.copyOf(dataProviders == null ? List.of() : dataProviders);
        if (id.isBlank()) throw new IllegalArgumentException("mod id must not be blank");
        if (version.isBlank()) throw new IllegalArgumentException("mod version must not be blank");
        if (entrypoints.isEmpty()) {
            throw new IllegalArgumentException("mod " + id + " declares no entrypoint");
        }
        if (!icon.isEmpty()) {
            if (icon.startsWith("/") || icon.contains("..")
                    || !icon.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
                throw new IllegalArgumentException("invalid icon path: " + icon);
            }
        }
    }

    public boolean supports(Environment runtime) {
        return environment == Environment.UNIVERSAL || environment == runtime;
    }

    /** Whether this mod declared a generated texture. */
    public boolean hasTexture() {
        return !texture.isEmpty();
    }

    /** Whether this mod declared a launcher icon. */
    public boolean hasIcon() {
        return !icon.isEmpty();
    }

    /** Whether this mod declared build-time/runtime data providers. */
    public boolean hasDataProviders() {
        return !dataProviders.isEmpty();
    }
}
