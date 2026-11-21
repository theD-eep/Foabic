package dev.foabic.api.mod;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface ModResources {

    Optional<InputStream> open(String path) throws IOException;

    default byte[] read(String path) throws IOException {
        try (InputStream in = open(path).orElseThrow(
                () -> new IOException("resource not found: " + path))) {
            return in.readAllBytes();
        }
    }

    boolean contains(String path);
}
