package dev.foabic.neoforge;

import dev.foabic.api.registry.CreativeTab;
import dev.foabic.api.registry.FoabicCreativeTabs;
import dev.foabic.api.registry.Registration;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Adds Foabic-registered items to the vanilla creative tabs on NeoForge. */
final class NeoForgeCreativeTabs {

    private NeoForgeCreativeTabs() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            String path = event.getTabKey().identifier().getPath();
            for (CreativeTab tab : CreativeTab.values()) {
                if (!tab.id().equals(path)) continue;
                for (Registration<? extends Item> registration : FoabicCreativeTabs.items(tab)) {
                    if (registration.isPresent()) {
                        event.accept(registration.get());
                    }
                }
            }
        });
    }
}
