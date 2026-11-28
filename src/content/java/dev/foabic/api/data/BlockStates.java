package dev.foabic.api.data;

import dev.foabic.api.data.internal.FoabicDataContextImpl;
import dev.foabic.api.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Fluent blockstate/model writer for one block.
 *
 * <p>The common case is one line:
 * <pre>{@code
 * context.blockStates(id).simple("mymod:block/generator");
 * }</pre>
 * This writes the blockstate, a cube-all block model and the matching item
 * model. More complex blocks can call {@link #variant} and {@link #model}.
 */
public final class BlockStates {

    private final FoabicDataContextImpl owner;
    private final ResourceLocation block;
    private final Map<String, Object> variants = new LinkedHashMap<>();
    private String defaultItemModel;

    public BlockStates(FoabicDataContextImpl owner, ResourceLocation block) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.block = Objects.requireNonNull(block, "block");
    }

    /** Writes a cube-all blockstate, block model and item model. */
    public BlockStates simple(String texture) {
        String model = modelId();
        model("minecraft:block/cube_all", Map.of("all", texture(texture)));
        variant("", model, 0, 0, false, 1);
        itemModel(model);
        return this;
    }

    /** Writes a cube-column blockstate and models (log/pillar style). */
    public BlockStates cubeColumn(String side, String end) {
        String model = modelId();
        model("minecraft:block/cube_column",
                Map.of("side", texture(side), "end", texture(end)));
        variant("", model, 0, 0, false, 1);
        itemModel(model);
        return this;
    }

    /** Writes a cross model for plants and saplings. */
    public BlockStates cross(String texture) {
        String model = modelId();
        model("minecraft:block/cross", Map.of("cross", texture(texture)));
        variant("", model, 0, 0, false, 1);
        itemModel(model);
        return this;
    }

    /** Writes an orientable blockstate with the four horizontal facing values. */
    public BlockStates orientable(String side, String front, String top) {
        String model = modelId();
        model("minecraft:block/orientable",
                Map.of("side", texture(side), "front", texture(front), "top", texture(top)));
        variant("facing=north", model, 0, 0, true, 1);
        variant("facing=east", model, 0, 90, true, 1);
        variant("facing=south", model, 0, 180, true, 1);
        variant("facing=west", model, 0, 270, true, 1);
        itemModel(model);
        return this;
    }

    /**
     * Adds one blockstate variant. The key is the vanilla property string, for
     * example {@code ""} for a simple block or {@code "facing=north"}.
     */
    public BlockStates variant(String key, String model, int x, int y,
                               boolean uvlock, int weight) {
        Objects.requireNonNull(key, "key");
        String modelId = model(model);
        Map<String, Object> variant = new LinkedHashMap<>();
        variant.put("model", modelId);
        if (x != 0) variant.put("x", x);
        if (y != 0) variant.put("y", y);
        if (uvlock) variant.put("uvlock", true);
        if (weight > 1) variant.put("weight", weight);

        Object previous = variants.get(key);
        if (previous == null) {
            variants.put(key, variant);
        } else if (previous instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list);
            copy.add(variant);
            variants.put(key, copy);
        } else {
            List<Object> list = new ArrayList<>();
            list.add(previous);
            list.add(variant);
            variants.put(key, list);
        }
        writeBlockstate();
        return this;
    }

    /** Writes a block model. */
    public BlockStates model(String parent, Map<String, String> textures) {
        Objects.requireNonNull(parent, "parent");
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("parent", model(parent));
        if (textures != null && !textures.isEmpty()) {
            Map<String, String> normalized = new LinkedHashMap<>();
            textures.forEach((key, value) -> normalized.put(key, texture(value)));
            json.put("textures", normalized);
        }
        owner.put("assets/" + block.namespace() + "/models/block/" + block.path() + ".json", json);
        return this;
    }

    /** Overrides the item model parent. */
    public BlockStates itemModel(String parent) {
        Objects.requireNonNull(parent, "parent");
        defaultItemModel = model(parent);
        owner.put("assets/" + block.namespace() + "/models/item/" + block.path() + ".json",
                Map.of("parent", defaultItemModel));
        return this;
    }

    public ResourceLocation block() {
        return block;
    }

    private void writeBlockstate() {
        owner.put("assets/" + block.namespace() + "/blockstates/" + block.path() + ".json",
                Map.of("variants", variants));
    }

    private String modelId() {
        return block.namespace() + ":block/" + block.path();
    }

    private String model(String value) {
        Objects.requireNonNull(value, "model");
        String text = value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("model must not be blank");
        if (!text.contains(":")) {
            if (text.startsWith("block/") || text.startsWith("item/")) {
                return "minecraft:" + text;
            }
            return block.namespace() + ":" + text;
        }
        ResourceLocation id = ResourceLocation.parse(text);
        return id.toString();
    }

    private String texture(String value) {
        Objects.requireNonNull(value, "texture");
        String text = value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("texture must not be blank");
        if (text.startsWith("assets/")) {
            int marker = text.indexOf("/textures/");
            if (marker < 0) {
                throw new IllegalArgumentException("texture path must contain /textures/: " + value);
            }
            text = text.substring(marker + "/textures/".length());
            if (text.endsWith(".png")) text = text.substring(0, text.length() - 4);
        }
        if (text.contains("..")) {
            throw new IllegalArgumentException("texture path must not contain ..: " + value);
        }
        if (!text.contains(":")) return block.namespace() + ":" + text;
        ResourceLocation id = ResourceLocation.parse(text);
        return id.toString();
    }
}
