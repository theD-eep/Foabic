package dev.foabic.api.config;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * A deliberately boring, zero-dependency properties config.
 *
 * <p>Values are strings on disk. Typed getters fail loudly on malformed input
 * instead of silently returning a default. Saving is explicit; call
 * {@link #save()} after changing values.
 */
public final class Config {

    private final Path path;
    private final Properties values = new Properties();

    private Config(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    public static Config load(Path path) {
        Config config = new Config(path);
        config.reload();
        return config;
    }

    public synchronized void reload() {
        values.clear();
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            values.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read config " + path, e);
        }
    }

    public synchronized String getString(String key, String fallback) {
        String value = values.getProperty(requireKey(key));
        return value == null ? fallback : value;
    }

    public synchronized int getInt(String key, int fallback) {
        String value = values.getProperty(requireKey(key));
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("config key '" + key + "' is not an int: " + value, e);
        }
    }

    public synchronized long getLong(String key, long fallback) {
        String value = values.getProperty(requireKey(key));
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("config key '" + key + "' is not a long: " + value, e);
        }
    }

    public synchronized double getDouble(String key, double fallback) {
        String value = values.getProperty(requireKey(key));
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("config key '" + key + "' is not a double: " + value, e);
        }
    }

    public synchronized boolean getBoolean(String key, boolean fallback) {
        String value = values.getProperty(requireKey(key));
        if (value == null) {
            return fallback;
        }
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true", "yes", "on" -> true;
            case "false", "no", "off" -> false;
            default -> throw new IllegalArgumentException(
                    "config key '" + key + "' is not a boolean: " + value);
        };
    }

    public synchronized void set(String key, Object value) {
        values.setProperty(requireKey(key), String.valueOf(Objects.requireNonNull(value, "value")));
    }

    public synchronized void remove(String key) {
        values.remove(requireKey(key));
    }

    public synchronized void save() {
        Path parent = path.getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new UncheckedIOException("could not create config directory " + parent, e);
            }
        }
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            values.store(writer, "Foabic configuration");
        } catch (IOException e) {
            throw new UncheckedIOException("could not write config " + path, e);
        }
    }

    public Path path() {
        return path;
    }

    private static String requireKey(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("config key must not be blank");
        }
        return key;
    }
}
