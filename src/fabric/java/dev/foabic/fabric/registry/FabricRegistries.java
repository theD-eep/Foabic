package dev.foabic.fabric.registry;

import dev.foabic.api.registry.FoabicRegistries;
import net.minecraft.core.registries.BuiltInRegistries;

/** Binds Foabic's platform-neutral registry views to Fabric's built-in registries. */
public final class FabricRegistries {

    private FabricRegistries() {
    }

    public static void bind() {
        FoabicRegistries.bind("blocks", new FabricRegistry<>(BuiltInRegistries.BLOCK));
        FoabicRegistries.bind("items", new FabricRegistry<>(BuiltInRegistries.ITEM));
    }
}
