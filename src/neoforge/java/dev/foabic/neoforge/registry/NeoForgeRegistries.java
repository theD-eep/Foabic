package dev.foabic.neoforge.registry;

import dev.foabic.api.registry.FoabicRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Binds Foabic's platform-neutral registry views to NeoForge/vanilla registries. */
public final class NeoForgeRegistries {

    public static NeoForgeRegistry<Block> BLOCKS;
    public static NeoForgeRegistry<Item> ITEMS;

    private NeoForgeRegistries() {
    }

    public static void bind() {
        BLOCKS = new NeoForgeRegistry<>(Registries.BLOCK, BuiltInRegistries.BLOCK);
        ITEMS = new NeoForgeRegistry<>(Registries.ITEM, BuiltInRegistries.ITEM);
        FoabicRegistries.bind("blocks", BLOCKS);
        FoabicRegistries.bind("items", ITEMS);
    }
}
