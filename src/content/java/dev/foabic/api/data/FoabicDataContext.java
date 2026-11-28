package dev.foabic.api.data;

import dev.foabic.api.util.ResourceLocation;

import java.util.Map;

/**
 * The writer passed to a {@link FoabicDataProvider}.
 *
 * <p>All methods write standard Minecraft JSON at the standard 26.3 data-pack
 * paths. A provider may be run at build time by {@code foabicData} and at
 * runtime by the Foabic loader; both produce the same files.
 */
public interface FoabicDataContext {

    /** Blockstate, block model and item-model helpers for one block. */
    BlockStates blockStates(ResourceLocation block);

    /** Writes a parent-only {@code assets/<namespace>/models/item/<path>.json}. */
    void itemModel(ResourceLocation item, String parent);

    /**
     * Writes an item model with textures, for example
     * {@code parent=item/generated, textures={layer0=mod:item/blade}}.
     */
    void itemModel(ResourceLocation item, String parent, Map<String, String> textures);

    /** A shaped, shapeless, cooking or stonecutting recipe. */
    RecipeBuilder recipe(ResourceLocation id);

    /** Adds entries to an item/block/entity-type/... tag. */
    TagBuilder tag(TagType type, ResourceLocation tag);

    /** A standard block loot table. */
    LootTableBuilder lootTable(ResourceLocation block);

    /** Translation entries, merged into {@code assets/<namespace>/lang/<locale>.json}. */
    LanguageBuilder lang(String locale);

    /**
     * Writes a raw JSON file under {@code assets/} or {@code data/}. This is the
     * escape hatch for features the typed builders do not cover (advancements,
     * worldgen, custom models).
     */
    void raw(String path, String json);

    /** Runs another provider against this context. */
    default void provider(FoabicDataProvider provider) {
        if (provider != null) provider.generate(this);
    }
}
