package dev.foabic.api.mod;

import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

import java.util.Objects;

/**
 * A declared dependency on another Foabic mod.
 *
 * @param id    the required mod id
 * @param range the accepted versions
 */
public record Dependency(String id, VersionRange range) {

    public Dependency {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(range, "range");
        if (id.isBlank()) {
            throw new IllegalArgumentException("dependency id must not be blank");
        }
    }

    public boolean matches(Version version) {
        return range.test(version);
    }
}
