package dev.foabic.fabric;

import dev.foabic.api.registry.CreativeTab;
import dev.foabic.api.registry.FoabicCreativeTabs;
import dev.foabic.api.registry.Registration;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.Item;

/** Adds Foabic-registered items to the vanilla creative tabs on Fabric. */
final class FabricCreativeTabs {

    private FabricCreativeTabs() {
    }

    static void register() {
        for (CreativeTab tab : CreativeTab.values()) {
            CreativeModeTabEvents.modifyOutputEvent(tab.key()).register(output -> {
                for (Registration<? extends Item> registration : FoabicCreativeTabs.items(tab)) {
                    if (registration.isPresent()) {
                        output.accept(registration.get());
                    }
                }
            });
        }
    }
}
