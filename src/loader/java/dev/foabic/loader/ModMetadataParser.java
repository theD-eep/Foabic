package dev.foabic.loader;

import dev.foabic.api.mod.Dependency;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModMetadata;
import dev.foabic.api.version.Version;
import dev.foabic.api.version.VersionRange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

final class ModMetadataParser {

    static final int MAX_BYTES = 64 * 1024;
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");

    private ModMetadataParser() {
    }

    static ModMetadata parse(byte[] bytes, String source) {
        if (bytes.length > MAX_BYTES) {
            throw new ModLoadException(source + ": foabic.mod is larger than 64 KiB");
        }
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            throw new ModLoadException(source + ": cannot parse foabic.mod", e);
        }

        String id = required(properties, "id", source);
        if (!ID.matcher(id).matches() || id.equals("foabic") || id.equals("minecraft")) {
            throw new ModLoadException(source + ": invalid or reserved mod id '" + id + "'");
        }
        String name = properties.getProperty("name", id).trim();
        if (name.isEmpty()) name = id;

        Version version;
        try {
            version = Version.parse(required(properties, "version", source));
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid version", e);
        }

        List<String> entrypoints = splitList(properties.getProperty(
                "entrypoints", properties.getProperty("entrypoint", "")));
        if (entrypoints.isEmpty()) throw new ModLoadException(source + ": missing 'entrypoint'");

        Environment environment;
        try {
            environment = Environment.parse(properties.getProperty("environment", "*"));
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid environment", e);
        }

        VersionRange apiRange;
        try {
            apiRange = VersionRange.parse(properties.getProperty("api", "^1.0.0"));
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid API range", e);
        }

        List<Dependency> dependencies = dependencies(properties.getProperty("depends", ""), source);
        List<Dependency> optional = dependencies(properties.getProperty("optional", ""), source);
        List<String> authors = splitList(properties.getProperty("authors", "").replace(';', ','));

        return new ModMetadata(id, name, version,
                properties.getProperty("description", "").trim(), authors, entrypoints,
                dependencies, optional, environment, apiRange);
    }

    private static List<Dependency> dependencies(String value, String source) {
        List<Dependency> result = new ArrayList<>();
        for (String token : splitList(value)) {
            String id;
            String rangeText;
            int at = token.indexOf('@');
            if (at < 0) {
                id = token;
                rangeText = "*";
            } else {
                id = token.substring(0, at).trim();
                rangeText = token.substring(at + 1).trim();
            }
            if (!ID.matcher(id).matches()) {
                throw new ModLoadException(source + ": invalid dependency id '" + id + "'");
            }
            try {
                result.add(new Dependency(id, VersionRange.parse(rangeText)));
            } catch (IllegalArgumentException e) {
                throw new ModLoadException(source + ": invalid range for dependency '" + id + "'", e);
            }
        }
        return result;
    }

    private static String required(Properties properties, String key, String source) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new ModLoadException(source + ": missing '" + key + "'");
        }
        return value.trim();
    }

    private static List<String> splitList(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
}
