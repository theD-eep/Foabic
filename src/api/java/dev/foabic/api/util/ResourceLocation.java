package dev.foabic.api.util;

import java.util.Objects;

/**
 * A Minecraft-style namespaced identifier, kept independent of Minecraft
 * classes so that common code never needs a game jar to compile.
 *
 * @param namespace the namespace, for example {@code minecraft}
 * @param path      the path, for example {@code block/stone}
 */
public record ResourceLocation(String namespace, String path) {

    private static final String NAMESPACE_PATTERN = "[a-z0-9_.-]+";
    private static final String PATH_PATTERN = "[a-z0-9/._-]+";

    public ResourceLocation {
        namespace = requirePart(namespace, "namespace", NAMESPACE_PATTERN);
        path = requirePart(path, "path", PATH_PATTERN);
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String value) {
        Objects.requireNonNull(value, "value");
        int colon = value.indexOf(':');
        if (colon < 0) {
            return new ResourceLocation("minecraft", value);
        }
        if (colon == 0 || colon == value.length() - 1) {
            throw new IllegalArgumentException("invalid resource location: " + value);
        }
        return new ResourceLocation(value.substring(0, colon), value.substring(colon + 1));
    }

    private static String requirePart(String value, String name, String pattern) {
        Objects.requireNonNull(value, name);
        if (value.isEmpty() || !value.matches(pattern)) {
            throw new IllegalArgumentException("invalid " + name + ": " + value);
        }
        return value;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
