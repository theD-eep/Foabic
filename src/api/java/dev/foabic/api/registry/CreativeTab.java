package dev.foabic.api.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

/**
 * The vanilla creative tabs Foabic can add items to. The adapter resolves the
 * tab by its stable registry id, so Foabic does not need platform-specific
 * creative-tab field names.
 */
public enum CreativeTab {

    BUILDING_BLOCKS("building_blocks"),
    NATURAL_BLOCKS("natural_blocks"),
    FUNCTIONAL_BLOCKS("functional_blocks"),
    REDSTONE_BLOCKS("redstone_blocks"),
    TOOLS_AND_UTILITIES("tools_and_utilities"),
    COMBAT("combat"),
    FOOD_AND_DRINKS("food_and_drinks"),
    INGREDIENTS("ingredients"),
    SPAWN_EGGS("spawn_eggs"),
    OP_BLOCKS("op_blocks");

    private final String id;

    CreativeTab(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Identifier identifier() {
        return Identifier.withDefaultNamespace(id);
    }

    public ResourceKey<CreativeModeTab> key() {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, identifier());
    }
}
