package dev.foabic.api.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The two handles created by {@link dev.foabic.api.mod.ModContext#submitBlock}.
 *
 * <p>The item handle is deferred on NeoForge and is evaluated in the item
 * registration event, after the block handle has been resolved.
 */
public record BlockItemRegistration(
        Registration<Block> block,
        Registration<Item> item
) {
}
