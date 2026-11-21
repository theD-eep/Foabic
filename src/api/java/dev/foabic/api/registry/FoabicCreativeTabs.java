package dev.foabic.api.registry;

import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Platform-neutral queue of items that should appear in a vanilla creative
 * tab. {@link ModContext#submitBlock} adds the generated block item here
 * automatically.
 *
 * <p>The queue is drained by each adapter when the platform builds creative
 * tab contents. A registration may still be deferred on NeoForge; the adapter
 * resolves it inside the creative-tab event, after {@code RegisterEvent}.
 */
public final class FoabicCreativeTabs {

    private static final Map<CreativeTab, List<Registration<? extends Item>>> ITEMS =
            new ConcurrentHashMap<>();
    private static final AtomicBoolean frozen = new AtomicBoolean();

    private FoabicCreativeTabs() {
    }

    public static void add(CreativeTab tab, Registration<? extends Item> item) {
        Objects.requireNonNull(tab, "tab");
        Objects.requireNonNull(item, "item");
        if (frozen.get()) {
            throw new IllegalStateException("creative tab queue is already frozen");
        }
        ITEMS.computeIfAbsent(tab, ignored -> new CopyOnWriteArrayList<>()).add(item);
    }

    /** Snapshot for an adapter's creative-tab callback. */
    public static List<Registration<? extends Item>> items(CreativeTab tab) {
        List<Registration<? extends Item>> list = ITEMS.get(tab);
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Prevents late additions after the game has started building tabs. */
    public static void freeze() {
        frozen.set(true);
    }

    public static void clear() {
        ITEMS.clear();
        frozen.set(false);
    }
}
