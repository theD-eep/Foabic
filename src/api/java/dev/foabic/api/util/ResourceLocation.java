package dev.foabic.api.util;

import java.util.Objects;

/** A Minecraft-style namespaced identifier, independent of Minecraft classes. */
public record ResourceLocation(String namespace, String path) {

    private static final String NAMESPACE = "[a-z0-9_.-]+";
    private static final String PATH = "[a-z0-9/._-]+";

    public ResourceLocation {
        namespace = requirePart(namespace, "namespace", NAMESPACE);
        path = requirePart(path, "path", PATH);
    }

    public static ResourceLocation of(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parse(String value) {
        Objects.requireNonNull(value, "value");
        int colon = value.indexOf(':');
        if (colon < 0) return new ResourceLocation("minecraft", value);
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
