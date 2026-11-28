package dev.foabic.api.data;

import dev.foabic.api.data.internal.FoabicDataContextImpl;
import dev.foabic.api.util.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Standard block loot tables.
 *
 * <pre>{@code
 * context.lootTable(id).dropsSelf();
 * }</pre>
 */
public final class LootTableBuilder {

    private final FoabicDataContextImpl owner;
    private final ResourceLocation block;

    public LootTableBuilder(FoabicDataContextImpl owner, ResourceLocation block) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.block = Objects.requireNonNull(block, "block");
    }

    /** Drops one of the block itself, surviving explosions. */
    public void dropsSelf() {
        drop(block.toString(), 1, 1);
    }

    /** Drops a fixed item id once. */
    public void drop(String item) {
        drop(item, 1, 1);
    }

    /** Drops a uniform random count of one item. */
    public void drop(String item, int min, int max) {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("invalid drop count " + min + ".." + max);
        }
        String itemId = ResourceLocation.parse(item).toString();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", "minecraft:item");
        entry.put("name", itemId);
        if (min != max) {
            Map<String, Object> count = new LinkedHashMap<>();
            count.put("type", "minecraft:uniform");
            count.put("min", min);
            count.put("max", max);
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("function", "minecraft:set_count");
            function.put("count", count);
            entry.put("functions", List.of(function));
        }

        Map<String, Object> pool = new LinkedHashMap<>();
        pool.put("rolls", 1);
        pool.put("entries", List.of(entry));
        pool.put("conditions", List.of(Map.of("condition", "minecraft:survives_explosion")));

        List<Object> pools = new ArrayList<>();
        pools.add(pool);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", "minecraft:block");
        json.put("pools", pools);
        owner.put("data/" + block.namespace() + "/loot_table/blocks/"
                + block.path() + ".json", json);
    }

    /** Writes an empty block loot table. */
    public void noDrop() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", "minecraft:block");
        json.put("pools", List.of());
        owner.put("data/" + block.namespace() + "/loot_table/blocks/"
                + block.path() + ".json", json);
    }

    public ResourceLocation block() {
        return block;
    }
}
