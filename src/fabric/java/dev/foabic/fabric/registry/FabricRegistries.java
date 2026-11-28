package dev.foabic.fabric.registry;

import dev.foabic.api.registry.FoabicRegistries;
import net.minecraft.core.registries.BuiltInRegistries;

/** Binds Foabic's platform-neutral registry views to Fabric's built-in registries. */
public final class FabricRegistries {

    private FabricRegistries() {
    }

    public static void bind() {
        bind("blocks", BuiltInRegistries.BLOCK);
        bind("items", BuiltInRegistries.ITEM);
        bind("block_entity_types", BuiltInRegistries.BLOCK_ENTITY_TYPE);
        bind("entity_types", BuiltInRegistries.ENTITY_TYPE);
        bind("sound_events", BuiltInRegistries.SOUND_EVENT);
    }

    private static <T> void bind(String name, net.minecraft.core.Registry<T> registry) {
        FoabicRegistries.bind(name, new FabricRegistry<>(registry));
    }
}
