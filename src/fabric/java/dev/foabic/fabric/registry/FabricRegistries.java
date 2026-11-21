package dev.foabic.fabric.registry;

import dev.foabic.api.registry.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Typed Foabic views over Minecraft's built-in registries.
 *
 * <p>Register blocks, items and other content during
 * {@code ModInitializer.onInitialize}; Minecraft freezes these registries
 * before the server starts. The adapter throws a descriptive
 * {@link dev.foabic.api.registry.RegistryException} if registration happens
 * after the freeze.
 */
public final class FabricRegistries {

    public static final Registry<Block> BLOCKS =
            new FabricRegistry<>(BuiltInRegistries.BLOCK);
    public static final Registry<Item> ITEMS =
            new FabricRegistry<>(BuiltInRegistries.ITEM);
    public static final Registry<EntityType<?>> ENTITY_TYPES =
            new FabricRegistry<>(BuiltInRegistries.ENTITY_TYPE);
    public static final Registry<SoundEvent> SOUND_EVENTS =
            new FabricRegistry<>(BuiltInRegistries.SOUND_EVENT);
    public static final Registry<ParticleType<?>> PARTICLE_TYPES =
            new FabricRegistry<>(BuiltInRegistries.PARTICLE_TYPE);
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            new FabricRegistry<>(BuiltInRegistries.BLOCK_ENTITY_TYPE);

    private FabricRegistries() {
    }
}
