package dev.foabic.neoforge.registry;

import dev.foabic.api.registry.FoabicRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Binds Foabic's platform-neutral registry views to NeoForge/vanilla registries. */
public final class NeoForgeRegistries {

    public static NeoForgeRegistry<Block> BLOCKS;
    public static NeoForgeRegistry<Item> ITEMS;
    public static NeoForgeRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES;
    public static NeoForgeRegistry<SoundEvent> SOUND_EVENTS;

    private NeoForgeRegistries() {
    }

    public static void bind() {
        BLOCKS = new NeoForgeRegistry<>(Registries.BLOCK, BuiltInRegistries.BLOCK);
        ITEMS = new NeoForgeRegistry<>(Registries.ITEM, BuiltInRegistries.ITEM);
        BLOCK_ENTITY_TYPES = new NeoForgeRegistry<>(
                Registries.BLOCK_ENTITY_TYPE, BuiltInRegistries.BLOCK_ENTITY_TYPE);
        SOUND_EVENTS = new NeoForgeRegistry<>(Registries.SOUND_EVENT, BuiltInRegistries.SOUND_EVENT);
        FoabicRegistries.bind("blocks", BLOCKS);
        FoabicRegistries.bind("items", ITEMS);
        FoabicRegistries.bind("block_entity_types", BLOCK_ENTITY_TYPES);
        FoabicRegistries.bind("sound_events", SOUND_EVENTS);
    }
}
