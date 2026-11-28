package dev.foabic.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.foabic.api.mod.Dependency;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModMetadata;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.loader.version.Version;
import dev.foabic.loader.version.VersionRange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Reads {@code foabic.mod.json} (preferred) or the legacy properties
 * {@code foabic.mod}. Both produce the same {@link ModMetadata}.
 */
final class ModMetadataParser {

    static final int MAX_BYTES = 64 * 1024;
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");

    private ModMetadataParser() {
    }

    static ModMetadata parse(byte[] bytes, String source) {
        if (bytes.length > MAX_BYTES) {
            throw new ModLoadException(source + ": metadata is larger than 64 KiB");
        }
        int offset = hasUtf8Bom(bytes) ? 3 : 0;
        String text = new String(bytes, offset, bytes.length - offset, StandardCharsets.UTF_8);
        if (text.stripLeading().startsWith("{")) {
            return parseJson(text, source);
        }
        return parseProperties(bytes, offset, source);
    }

    private static ModMetadata parseJson(String text, String source) {
        JsonObject json;
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                throw new ModLoadException(source + ": foabic.mod.json must be a JSON object");
            }
            json = parsed.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            throw new ModLoadException(source + ": cannot parse foabic.mod.json", e);
        }
        int schema = integer(json, "schemaVersion", 1, source);
        if (schema != 1) {
            throw new ModLoadException(source + ": unsupported metadata schemaVersion " + schema);
        }

        String id = requiredString(json, "id", source);
        validateId(id, source);
        String name = optionalString(json, "name", id);
        String versionText = requiredString(json, "version", source);
        Version version;
        try {
            version = Version.parse(versionText);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid version", e);
        }
        List<String> entrypoints = entrypoints(json.get("entrypoint"));
        if (entrypoints.isEmpty()) entrypoints = entrypoints(json.get("entrypoints"));
        if (entrypoints.isEmpty()) {
            throw new ModLoadException(source + ": missing 'entrypoint'");
        }
        Environment environment;
        try {
            environment = Environment.parse(optionalString(json, "environment", "*"));
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid environment", e);
        }
        String apiRangeText = optionalString(json, "api", "*");
        VersionRange apiRange;
        try {
            apiRange = VersionRange.parse(apiRangeText);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid API range", e);
        }
        List<Dependency> dependencies = dependencies(json.get("depends"), source);
        JsonElement optional = json.has("optionalDepends")
                ? json.get("optionalDepends") : json.get("optional");
        List<Dependency> optionalDependencies = dependencies(optional, source);
        List<String> authors = strings(json.get("authors"));
        String texture = optionalString(json, "texture", "");
        String icon = normalizeIcon(optionalString(json, "icon", ""), source);
        JsonElement data = json.has("data") ? json.get("data") : json.get("dataProviders");
        List<String> dataProviders = classNames(data, source);

        return new ModMetadata(id, name, versionText, optionalString(json, "description", ""),
                authors, entrypoints, dependencies, optionalDependencies, environment,
                apiRangeText, normalizeTexture(texture, id, source), icon, dataProviders);
    }

    private static ModMetadata parseProperties(byte[] bytes, int offset, String source) {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(bytes, offset, bytes.length - offset),
                StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            throw new ModLoadException(source + ": cannot parse foabic.mod", e);
        }

        String id = required(properties, "id", source);
        validateId(id, source);
        String name = properties.getProperty("name", id).trim();
        if (name.isEmpty()) name = id;

        String versionText = required(properties, "version", source);
        try {
            Version.parse(versionText);
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

        String apiRange = properties.getProperty("api", "^1.0.0").trim();
        try {
            VersionRange.parse(apiRange);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid API range", e);
        }

        List<Dependency> dependencies = dependencies(properties.getProperty("depends", ""), source);
        List<Dependency> optional = dependencies(properties.getProperty("optional", ""), source);
        List<String> authors = splitList(properties.getProperty("authors", "").replace(';', ','));
        String texture = normalizeTexture(properties.getProperty("texture", ""), id, source);
        String icon = normalizeIcon(properties.getProperty("icon", ""), source);
        List<String> dataProviders = classNames(properties.getProperty("data", ""), source);

        return new ModMetadata(id, name, versionText,
                properties.getProperty("description", "").trim(), authors, entrypoints,
                dependencies, optional, environment, apiRange, texture, icon, dataProviders);
    }

    private static void validateId(String id, String source) {
        if (!ID.matcher(id).matches() || id.equals("foabic") || id.equals("minecraft")) {
            throw new ModLoadException(source + ": invalid or reserved mod id '" + id + "'");
        }
    }

    private static List<Dependency> dependencies(JsonElement element, String source) {
        List<Dependency> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;
        if (!element.isJsonObject()) {
            throw new ModLoadException(source + ": 'depends' must be a JSON object");
        }
        for (var entry : element.getAsJsonObject().entrySet()) {
            String id = entry.getKey();
            if (!ID.matcher(id).matches()) {
                throw new ModLoadException(source + ": invalid dependency id '" + id + "'");
            }
            String range = entry.getValue().isJsonPrimitive()
                    ? entry.getValue().getAsString() : "*";
            try {
                VersionRange.parse(range);
            } catch (IllegalArgumentException e) {
                throw new ModLoadException(source + ": invalid range for dependency '" + id + "'", e);
            }
            result.add(new Dependency(id, range));
        }
        return result;
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
                VersionRange.parse(rangeText);
            } catch (IllegalArgumentException e) {
                throw new ModLoadException(source + ": invalid range for dependency '" + id + "'", e);
            }
            result.add(new Dependency(id, rangeText));
        }
        return result;
    }

    private static List<String> entrypoints(JsonElement element) {
        if (element == null || element.isJsonNull()) return List.of();
        if (element.isJsonPrimitive()) {
            String value = element.getAsString().trim();
            return value.isEmpty() ? List.of() : List.of(value);
        }
        return strings(element);
    }

    private static List<String> classNames(JsonElement element, String source) {
        List<String> values = strings(element);
        for (String value : values) {
            if (!value.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
                throw new ModLoadException(source + ": invalid data provider class '" + value + "'");
            }
        }
        return values;
    }

    private static List<String> classNames(String value, String source) {
        List<String> values = splitList(value);
        for (String name : values) {
            if (!name.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*")) {
                throw new ModLoadException(source + ": invalid data provider class '" + name + "'");
            }
        }
        return values;
    }

    private static List<String> strings(JsonElement element) {
        List<String> result = new ArrayList<>();
        if (element == null || element.isJsonNull()) return result;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                if (entry.isJsonPrimitive()) {
                    String value = entry.getAsString().trim();
                    if (!value.isEmpty()) result.add(value);
                }
            }
        } else if (element.isJsonPrimitive()) {
            for (String part : element.getAsString().split(",")) {
                if (!part.isBlank()) result.add(part.trim());
            }
        }
        return result;
    }

    private static String normalizeTexture(String value, String id, String source) {
        String texture = value == null ? "" : value.trim();
        if (texture.isEmpty()) return "";
        try {
            if (!texture.contains(":")) texture = id + ":" + texture;
            return ResourceLocation.parse(texture).toString();
        } catch (IllegalArgumentException e) {
            throw new ModLoadException(source + ": invalid texture id '" + texture + "'", e);
        }
    }

    private static String normalizeIcon(String value, String source) {
        String icon = value == null ? "" : value.trim().replace('\\', '/');
        if (icon.isEmpty()) return "";
        if (icon.startsWith("/") || icon.contains("..")
                || !icon.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
            throw new ModLoadException(source + ": invalid icon path '" + icon + "'");
        }
        return icon;
    }

    private static String requiredString(JsonObject json, String key, String source) {
        String value = optionalString(json, key, null);
        if (value == null) throw new ModLoadException(source + ": missing '" + key + "'");
        return value;
    }

    private static String optionalString(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) return fallback;
        String value = element.getAsString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private static int integer(JsonObject json, String key, int fallback, String source) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) return fallback;
        try {
            return element.getAsInt();
        } catch (RuntimeException e) {
            throw new ModLoadException(source + ": '" + key + "' must be an integer", e);
        }
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

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xff) == 0xef
                && (bytes[1] & 0xff) == 0xbb
                && (bytes[2] & 0xff) == 0xbf;
    }
}
