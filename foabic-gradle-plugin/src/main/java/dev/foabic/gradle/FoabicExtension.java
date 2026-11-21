package dev.foabic.gradle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The {@code foabic { }} extension. All values are optional overrides of foabic.mod.json. */
public class FoabicExtension {

    private String minecraftVersion = "26.3";
    private String id;
    private String version;
    private String name;
    private String entrypoint;
    private String environment;
    private String description;
    private String license;
    private List<String> authors;
    private String texture;
    private String icon;
    private final Map<String, String> depends = new LinkedHashMap<>();
    private final Map<String, String> optionalDepends = new LinkedHashMap<>();

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntrypoint() {
        return entrypoint;
    }

    public void setEntrypoint(String entrypoint) {
        this.entrypoint = entrypoint;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    /** PNG path inside the jar, for example {@code icon.png} or {@code assets/mymod/icon.png}. */
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Map<String, String> getDepends() {
        return depends;
    }

    public Map<String, String> getOptionalDepends() {
        return optionalDepends;
    }

    public void depends(String id, String range) {
        depends.put(id, range);
    }

    public void optionalDepends(String id, String range) {
        optionalDepends.put(id, range);
    }

    Map<String, Object> overrides() {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "id", id);
        put(values, "version", version);
        put(values, "name", name);
        put(values, "entrypoint", entrypoint);
        put(values, "environment", environment);
        put(values, "description", description);
        put(values, "license", license);
        put(values, "texture", texture);
        put(values, "icon", icon);
        if (authors != null) values.put("authors", new ArrayList<>(authors));
        if (!depends.isEmpty()) values.put("depends", new LinkedHashMap<>(depends));
        if (!optionalDepends.isEmpty()) values.put("optionalDepends", new LinkedHashMap<>(optionalDepends));
        return values;
    }

    private static void put(Map<String, Object> values, String key, String value) {
        if (value != null && !value.isBlank()) values.put(key, value);
    }
}
