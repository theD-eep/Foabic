package dev.foabic.api.mod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Read-only access to this mod's jar, scoped to this mod only.
 */
public interface ModResources {

    /**
     * Opens a resource relative to the mod jar root.
     *
     * @param path a slash-separated path such as {@code data/example/recipe.json}
     * @return the stream, or empty when the entry does not exist
     */
    Optional<InputStream> open(String path) throws IOException;

    default byte[] read(String path) throws IOException {
        try (InputStream in = open(path).orElseThrow(
                () -> new IOException("resource not found: " + path))) {
            return in.readAllBytes();
        }
    }

    boolean contains(String path);
}
