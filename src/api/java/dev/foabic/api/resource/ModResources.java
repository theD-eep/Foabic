package dev.foabic.api.resource;

import dev.foabic.api.util.ResourceLocation;

/**
 * Per-mod generated resource requests.
 *
 * <p>The normal path is metadata-only: set {@code texture=namespace:block/foo}
 * in {@code foabic.mod}, call {@link dev.foabic.api.mod.ModContext#submitBlock},
 * and Foabic generates the blockstate, block model and item model. This API is
 * the one-line code path for a different texture/model per block.
 *
 * <p>Generated files are placed in a built-in resource pack, so no manual
 * {@code src/main/resources} layout is required. A block with no declared
 * texture and no explicit request still receives a generated fallback model
 * instead of the purple-black missing texture.
 */
public interface ModResources {

    /**
     * Generates a cube-all blockstate, block model and item model.
     *
     * @param blockId the block's registry id
     * @param texture a texture id such as {@code mymod:block/stone}
     */
    default void block(ResourceLocation blockId, String texture) {
        block(blockId, texture, "minecraft:block/cube_all");
    }

    /**
     * Generates a blockstate and models with a custom model parent. The
     * generated block model always supplies an {@code all} texture, so the
     * parent must accept that key (for example {@code minecraft:block/cube_all}).
     *
     * @param blockId    the block's registry id
     * @param texture    a texture id such as {@code mymod:block/stone}
     * @param modelParent a model parent such as {@code minecraft:block/cube_all}
     */
    void block(ResourceLocation blockId, String texture, String modelParent);

    /** Overrides the generated item model parent for an item id. */
    void item(ResourceLocation itemId, ResourceLocation model);

    /** A no-op sink used before a runtime is installed. */
    static ModResources noop() {
        return new ModResources() {
            @Override
            public void block(ResourceLocation blockId, String texture, String modelParent) {
                // no runtime
            }

            @Override
            public void item(ResourceLocation itemId, ResourceLocation model) {
                // no runtime
            }
        };
    }
}
