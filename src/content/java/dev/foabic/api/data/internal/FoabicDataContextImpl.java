package dev.foabic.api.data.internal;

import dev.foabic.api.data.BlockStates;
import dev.foabic.api.data.FoabicDataContext;
import dev.foabic.api.data.LanguageBuilder;
import dev.foabic.api.data.LootTableBuilder;
import dev.foabic.api.data.RecipeBuilder;
import dev.foabic.api.data.TagBuilder;
import dev.foabic.api.data.TagType;
import dev.foabic.api.util.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Collects generated files in memory, merges tags and language entries, then
 * writes them below a data-pack root.
 *
 * <p>This class is public only because the loader is a separate module. Mods
 * should use {@link FoabicDataContext}.
 */
public final class FoabicDataContextImpl implements FoabicDataContext {

    private final String modId;
    private final Map<String, String> files = new TreeMap<>();
    private final Map<String, TagModel> tags = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> languages = new LinkedHashMap<>();

    public FoabicDataContextImpl(String modId) {
        this.modId = Objects.requireNonNull(modId, "modId");
    }

    @Override
    public BlockStates blockStates(ResourceLocation block) {
        return new BlockStates(this, block);
    }

    @Override
    public void itemModel(ResourceLocation item, String parent) {
        itemModel(item, parent, Map.of());
    }

    @Override
    public void itemModel(ResourceLocation item, String parent, Map<String, String> textures) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(parent, "parent");
        String model = parent.trim();
        if (model.isEmpty()) throw new IllegalArgumentException("item model parent is blank");
        if (!model.contains(":")) {
            model = (model.startsWith("item/") || model.startsWith("block/"))
                    ? "minecraft:" + model : item.namespace() + ":" + model;
        }
        ResourceLocation parsed = ResourceLocation.parse(model);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("parent", parsed.toString());
        if (textures != null && !textures.isEmpty()) {
            Map<String, String> normalized = new LinkedHashMap<>();
            textures.forEach((key, value) -> {
                String texture = value.trim();
                if (!texture.contains(":")) texture = item.namespace() + ":" + texture;
                normalized.put(key, ResourceLocation.parse(texture).toString());
            });
            json.put("textures", normalized);
        }
        put("assets/" + item.namespace() + "/models/item/" + item.path() + ".json", json);
    }

    @Override
    public RecipeBuilder recipe(ResourceLocation id) {
        return new RecipeBuilder(this, id);
    }

    @Override
    public TagBuilder tag(TagType type, ResourceLocation tag) {
        return new TagBuilder(this, type, tag);
    }

    @Override
    public LootTableBuilder lootTable(ResourceLocation block) {
        return new LootTableBuilder(this, block);
    }

    @Override
    public LanguageBuilder lang(String locale) {
        return new LanguageBuilder(this, locale);
    }

    @Override
    public void raw(String path, String json) {
        putRaw(path, json);
    }

    /** Stores a generated JSON file. Package-facing for the builders. */
    public void put(String path, Object json) {
        Objects.requireNonNull(path, "path");
        validatePath(path);
        files.put(path, Json.write(json));
    }

    /** Stores a raw generated JSON string. */
    public void putRaw(String path, String json) {
        Objects.requireNonNull(path, "path");
        validatePath(path);
        files.put(path, Objects.requireNonNull(json, "json"));
    }

    public String modId() {
        return modId;
    }

    public Map<String, String> files() {
        finish();
        return Map.copyOf(files);
    }

    public boolean isEmpty() {
        finish();
        return files.isEmpty();
    }

    /** Writes every generated file below {@code root}. */
    public int write(Path root) {
        Objects.requireNonNull(root, "root");
        finish();
        Path normalized = root.toAbsolutePath().normalize();
        try {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                Path target = normalized.resolve(entry.getKey()).normalize();
                if (!target.startsWith(normalized)) {
                    throw new IllegalArgumentException(
                            "generated data path escapes root: " + entry.getKey());
                }
                Files.createDirectories(target.getParent());
                Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot write generated data to " + normalized, e);
        }
        return files.size();
    }

    /** Adds a tag entry; repeated calls merge instead of overwriting. */
    public void addTag(TagType type, ResourceLocation tag, String value,
                       boolean required, boolean replace) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(value, "value");
        String path = "data/" + tag.namespace() + "/tags/" + type.directory()
                + "/" + tag.path() + ".json";
        TagModel model = tags.computeIfAbsent(path, ignored -> new TagModel());
        model.replace |= replace;
        if (!value.isEmpty()) model.values.put(value, required);
        files.put(path, Json.write(model.toJson()));
    }

    /** Adds or replaces one translation entry. */
    public void addLanguage(String locale, String key, String value) {
        Objects.requireNonNull(locale, "locale");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        String language = locale.trim().toLowerCase(java.util.Locale.ROOT);
        if (!language.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("invalid language code: " + locale);
        }
        languages.computeIfAbsent(modId + "/" + language, ignored -> new LinkedHashMap<>())
                .put(key, value);
    }

    public int fileCount() {
        finish();
        return files.size();
    }

    public long dataFileCount() {
        finish();
        return files.keySet().stream().filter(name -> name.startsWith("data/")).count();
    }

    public long assetFileCount() {
        finish();
        return files.keySet().stream().filter(name -> name.startsWith("assets/")).count();
    }

    private void finish() {
        for (Map.Entry<String, Map<String, String>> entry : languages.entrySet()) {
            String key = entry.getKey();
            int slash = key.indexOf('/');
            String namespace = key.substring(0, slash);
            String locale = key.substring(slash + 1);
            String path = "assets/" + namespace + "/lang/" + locale + ".json";
            files.put(path, Json.write(new LinkedHashMap<>(entry.getValue())));
        }
    }

    private static void validatePath(String path) {
        if (path.startsWith("/") || path.contains("..") || path.contains("\\")
                || !(path.startsWith("assets/") || path.startsWith("data/"))) {
            throw new IllegalArgumentException("invalid generated data path: " + path);
        }
    }

    private static final class TagModel {
        private final Map<String, Boolean> values = new LinkedHashMap<>();
        private boolean replace;

        private Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            if (replace) json.put("replace", true);
            json.put("values", values.entrySet().stream().map(entry -> {
                if (entry.getValue()) return entry.getKey();
                Map<String, Object> optional = new LinkedHashMap<>();
                optional.put("id", entry.getKey());
                optional.put("required", false);
                return optional;
            }).toList());
            return json;
        }
    }
}
