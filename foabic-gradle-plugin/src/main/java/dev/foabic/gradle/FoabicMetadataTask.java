package dev.foabic.gradle;

import groovy.json.JsonOutput;
import groovy.json.JsonSlurper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Generates the single {@code foabic.mod.json}, then translates it into the
 * Fabric and NeoForge metadata files. Runs before both platform jars.
 */
public class FoabicMetadataTask extends DefaultTask {

    private static final Pattern MOD_ID = Pattern.compile("[a-z][a-z0-9_-]{0,63}");

    private File metadataFile;
    private File resourcesDirectory;
    private File outputRoot;
    private FoabicExtension extension;
    private String projectName;
    private String projectVersion;

    @OutputDirectory
    public File getOutputRoot() {
        return outputRoot;
    }

    public void setMetadataFile(File metadataFile) {
        this.metadataFile = metadataFile;
    }

    public void setResourcesDirectory(File resourcesDirectory) {
        this.resourcesDirectory = resourcesDirectory;
    }

    public void setOutputRoot(File outputRoot) {
        this.outputRoot = outputRoot;
    }

    public void setExtension(FoabicExtension extension) {
        this.extension = extension;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    @TaskAction
    public void generate() throws IOException {
        Objects.requireNonNull(outputRoot, "outputRoot");
        if (outputRoot.exists() && !deleteRecursively(outputRoot.toPath())) {
            throw new GradleException("Cannot clean Foabic metadata directory " + outputRoot);
        }

        Map<String, Object> source = readSource();
        Map<String, Object> overrides = extension == null ? Map.of() : extension.overrides();
        Map<String, Object> metadata = new LinkedHashMap<>(source);
        metadata.putAll(overrides);

        String id = string(metadata.get("id"));
        if (id == null) id = sanitizeId(projectName);
        if (id == null || !MOD_ID.matcher(id).matches()
                || id.equals("foabic") || id.equals("minecraft")) {
            throw new GradleException("foabic.mod.json needs a valid mod id, got: " + id);
        }
        String version = string(metadata.get("version"));
        if (version == null) version = projectVersion;
        if (version == null || version.isBlank()) {
            throw new GradleException("foabic.mod.json needs a non-empty version");
        }
        String name = string(metadata.get("name"));
        if (name == null) name = projectName;
        String entrypoint = entrypoint(metadata);
        if (entrypoint == null) {
            throw new GradleException("foabic.mod.json needs entrypoint (or entrypoints)");
        }
        String description = string(metadata.get("description"));
        if (description == null) description = "";
        String environment = string(metadata.get("environment"));
        if (environment == null) environment = "*";
        String license = string(metadata.get("license"));
        if (license == null) license = "MIT";
        String texture = string(metadata.get("texture"));
        String icon = icon(metadata, id);
        String api = string(metadata.get("api"));
        if (api == null) api = "^1.2.0";
        List<String> authors = strings(metadata.get("authors"));
        Map<String, String> depends = dependencies(metadata.get("depends"));
        Map<String, String> optional = dependencies(metadata.get("optionalDepends"));
        if (optional.isEmpty()) optional = dependencies(metadata.get("optional"));

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("schemaVersion", 1);
        normalized.put("id", id);
        normalized.put("version", version);
        normalized.put("name", name);
        normalized.put("description", description);
        normalized.put("authors", authors);
        normalized.put("license", license);
        normalized.put("environment", environment);
        normalized.put("entrypoint", entrypoint);
        normalized.put("api", api);
        if (texture != null && !texture.isBlank()) normalized.put("texture", texture);
        if (icon != null) normalized.put("icon", icon);
        normalized.put("depends", depends);
        if (!optional.isEmpty()) normalized.put("optionalDepends", optional);

        Path common = outputRoot.toPath().resolve("common");
        Path fabric = outputRoot.toPath().resolve("fabric");
        Path neoForge = outputRoot.toPath().resolve("neoforge").resolve("META-INF");
        write(common.resolve("foabic.mod.json"), JsonOutput.prettyPrint(JsonOutput.toJson(normalized)) + "\n");
        write(fabric.resolve("fabric.mod.json"), fabricJson(normalized, id, version, name,
                description, authors, license, environment, depends, optional));
        write(neoForge.resolve("neoforge.mods.toml"), neoForgeToml(normalized, id, version,
                name, description, authors, license, environment, depends, optional));
    }

    private Map<String, Object> readSource() {
        if (metadataFile == null || !metadataFile.isFile()) return Map.of();
        Object parsed;
        try {
            parsed = new JsonSlurper().parse(metadataFile, StandardCharsets.UTF_8.name());
        } catch (RuntimeException e) {
            throw new GradleException("Cannot parse " + metadataFile + ": " + e.getMessage(), e);
        }
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new GradleException(metadataFile + " must contain a JSON object");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private String fabricJson(Map<String, Object> normalized, String id, String version,
                              String name, String description, List<String> authors,
                              String license, String environment,
                              Map<String, String> depends, Map<String, String> optional) {
        Map<String, Object> fabricDepends = new LinkedHashMap<>();
        fabricDepends.put("fabricloader", ">=0.20.1");
        fabricDepends.put("minecraft", "~" + extensionMinecraftVersion());
        fabricDepends.put("fabric-api", ">=0.166.2");
        fabricDepends.put("foabic", ">=1.2.0");
        depends.forEach((key, value) -> {
            if (!key.equals("neoforge") && !key.equals("minecraft")
                    && !key.equals("fabricloader") && !key.equals("fabric-api")) {
                fabricDepends.put(key, value);
            }
        });
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("schemaVersion", 1);
        json.put("id", id);
        json.put("version", version);
        json.put("name", name);
        json.put("description", description);
        json.put("authors", authors);
        json.put("license", license);
        if (normalized.get("icon") instanceof String icon) {
            json.put("icon", icon);
        }
        json.put("environment", switch (environment.toLowerCase()) {
            case "client" -> "client";
            case "server" -> "server";
            default -> "*";
        });
        json.put("depends", fabricDepends);
        if (!optional.isEmpty()) {
            Map<String, String> optionalCopy = new LinkedHashMap<>();
            optional.forEach((key, value) -> {
                if (!key.equals("neoforge") && !key.equals("minecraft")
                        && !key.equals("fabricloader") && !key.equals("fabric-api")) {
                    optionalCopy.put(key, value);
                }
            });
            if (!optionalCopy.isEmpty()) json.put("suggests", optionalCopy);
        }
        json.put("custom", Map.of("foabic", normalized));
        return JsonOutput.prettyPrint(JsonOutput.toJson(json)) + "\n";
    }

    private String neoForgeToml(Map<String, Object> normalized, String id, String version,
                                String name, String description, List<String> authors,
                                String license, String environment,
                                Map<String, String> depends, Map<String, String> optional) {
        String side = switch (environment.toLowerCase()) {
            case "client" -> "CLIENT";
            case "server" -> "SERVER";
            default -> "BOTH";
        };
        StringBuilder out = new StringBuilder();
        out.append("modLoader=\"javafml\"\n");
        out.append("loaderVersion=\"[4,)\"\n");
        out.append("license=").append(toml(license)).append("\n");
        out.append("foabicMetadata=").append(toml(JsonOutput.toJson(normalized))).append("\n\n");
        out.append("[[mods]]\n");
        out.append("modId=").append(toml(id)).append("\n");
        out.append("version=").append(toml(version)).append("\n");
        out.append("displayName=").append(toml(name)).append("\n");
        out.append("authors=").append(toml(String.join(", ", authors))).append("\n");
        if (normalized.get("icon") instanceof String icon) {
            out.append("logoFile=").append(toml(icon)).append("\n");
        }
        out.append("description=").append(toml(description)).append("\n\n");
        dependency(out, id, "neoforge", "[26.3.0.115,)", "required", side);
        dependency(out, id, "minecraft", "[26.3,26.4)", "required", side);
        dependency(out, id, "foabic", "[1.2.0,)", "required", side);
        depends.forEach((key, value) -> {
            if (!key.equals("fabric-api") && !key.equals("fabricloader")
                    && !key.equals("minecraft") && !key.equals("neoforge")
                    && !key.equals("foabic")) {
                dependency(out, id, key, tomlVersionRange(value), "required", side);
            }
        });
        optional.forEach((key, value) -> {
            if (!key.equals("fabric-api") && !key.equals("fabricloader")
                    && !key.equals("minecraft") && !key.equals("neoforge")
                    && !key.equals("foabic")) {
                dependency(out, id, key, tomlVersionRange(value), "optional", side);
            }
        });
        return out.toString();
    }

    private static void dependency(StringBuilder out, String owner, String id, String range,
                                   String type, String side) {
        out.append("[[dependencies.").append(owner).append("]]\n");
        out.append("modId=").append(toml(id)).append("\n");
        out.append("type=").append(toml(type)).append("\n");
        out.append("versionRange=").append(toml(range)).append("\n");
        out.append("ordering=\"NONE\"\n");
        out.append("side=").append(toml(side)).append("\n\n");
    }

    private static String tomlVersionRange(String range) {
        if (range == null || range.isBlank() || range.equals("*")) return "[0,)";
        if (range.startsWith("^") || range.startsWith("~")) return "[" + range.substring(1) + ",)";
        return range;
    }

    private String extensionMinecraftVersion() {
        return extension == null ? "26.3" : extension.getMinecraftVersion();
    }

    private String icon(Map<String, Object> metadata, String id) {
        String configured = string(metadata.get("icon"));
        if (configured != null) {
            return validateIcon(configured);
        }
        Path resources = resourcesDirectory == null
                ? (metadataFile == null ? null : metadataFile.toPath().getParent())
                : resourcesDirectory.toPath();
        if (resources == null) return null;
        for (String candidate : List.of("icon.png", "assets/" + id + "/icon.png")) {
            if (Files.isRegularFile(resources.resolve(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private String validateIcon(String value) {
        String icon = value.trim().replace('\\', '/');
        if (icon.startsWith("/") || icon.contains("..") || !icon.toLowerCase().endsWith(".png")) {
            throw new GradleException("Invalid Foabic icon path: " + value);
        }
        Path resources = resourcesDirectory == null
                ? (metadataFile == null ? null : metadataFile.toPath().getParent())
                : resourcesDirectory.toPath();
        if (resources == null || !Files.isRegularFile(resources.resolve(icon))) {
            throw new GradleException("Foabic icon not found in resources: " + icon
                    + " (expected under " + resources + ")");
        }
        return icon;
    }

    private static String entrypoint(Map<String, Object> metadata) {
        String direct = string(metadata.get("entrypoint"));
        if (direct != null) return direct;
        List<String> entries = strings(metadata.get("entrypoints"));
        return entries.isEmpty() ? null : entries.get(0);
    }

    private static Map<String, String> dependencies(Object value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, range) -> {
                if (key != null && range != null) {
                    result.put(String.valueOf(key), String.valueOf(range));
                }
            });
        }
        return result;
    }

    private static String string(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> strings(Object value) {
        List<String> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                if (entry != null && !String.valueOf(entry).isBlank()) {
                    result.add(String.valueOf(entry));
                }
            }
        } else if (value != null) {
            for (String part : String.valueOf(value).split(",")) {
                if (!part.isBlank()) result.add(part.trim());
            }
        }
        return result;
    }

    private static String sanitizeId(String name) {
        if (name == null) return null;
        String value = name.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        return value.matches("[a-z].*") ? value : "mod_" + value;
    }

    private static String toml(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private static void write(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value, StandardCharsets.UTF_8);
    }

    private static boolean deleteRecursively(Path root) {
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
