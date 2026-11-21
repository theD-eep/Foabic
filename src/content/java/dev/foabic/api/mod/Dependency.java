package dev.foabic.api.mod;

import java.util.Objects;

/** One declared mod dependency and its version range expression. */
public record Dependency(String id, String range) {

    public Dependency {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(range, "range");
        if (id.isBlank()) throw new IllegalArgumentException("dependency id must not be blank");
        if (range.isBlank()) throw new IllegalArgumentException("dependency range must not be blank");
    }
}
