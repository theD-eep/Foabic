package dev.foabic.api.registry;

import dev.foabic.api.util.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Platform-neutral registry views.
 *
 * <p>Mods use the same constants on Fabric and NeoForge. The active adapter
 * binds the concrete platform view before any Foabic mod is initialized, so
 * loading a mod never requires a platform-specific import.
 *
 * <p>The v1.2 surface contains blocks, items, block-entity types and sound
 * events. Only these four are supported; arbitrary platform registries are not
 * part of the stable API.
 */
public final class FoabicRegistries {

    private static final System.Logger LOG = System.getLogger("Foabic");
    private static final Map<String, Registry<?>> REGISTRIES = new ConcurrentHashMap<>();
    private static volatile boolean frozen;

    public static final Registry<Block> BLOCKS = new Proxy<>("blocks");
    public static final Registry<Item> ITEMS = new Proxy<>("items");
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            new Proxy<>("block_entity_types");
    public static final Registry<SoundEvent> SOUND_EVENTS = new Proxy<>("sound_events");

    private FoabicRegistries() {
    }

    /**
     * Binds a platform registry view. Called by the Fabric/NeoForge adapter
     * exactly once during startup, before {@code FoabicLoader.load}.
     */
    public static void bind(String name, Registry<?> registry) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(registry, "registry");
        Registry<?> previous = REGISTRIES.putIfAbsent(name, registry);
        if (previous != null) {
            throw new IllegalStateException("Foabic registry '" + name + "' is already bound");
        }
    }

    /** Opens the registration window for a new loader session. */
    public static void beginRegistration() {
        frozen = false;
    }

    /** Closes the registration window. Adapters call this once after bootstrap. */
    public static void freezeRegistration() {
        frozen = true;
    }

    public static boolean isFrozen() {
        return frozen;
    }

    /**
     * Guards a submission. Logs the required actionable line and throws when
     * the registration window is closed.
     */
    public static void checkOpen(String modId, ResourceLocation id, System.Logger logger) {
        if (!frozen) return;
        String message = "[Foabic] 模组 " + modId + " 试图在注册表冻结后注册物品 "
                + id + "，已被拦截。";
        logger.log(System.Logger.Level.ERROR, message);
        throw new FoabicRegistryFrozenException(modId, String.valueOf(id), message);
    }

    /** Clears all bindings and reopens the state. Used on startup failure or by tests. */
    public static void unbindAll() {
        REGISTRIES.clear();
        frozen = false;
    }

    private static Registry<?> delegate(String name) {
        Registry<?> registry = REGISTRIES.get(name);
        if (registry == null) {
            throw new IllegalStateException("Foabic registry '" + name
                    + "' is not bound; is a platform adapter installed?");
        }
        return registry;
    }

    private static final class Proxy<T> implements Registry<T> {

        private final String name;

        private Proxy(String name) {
            this.name = name;
        }

        @SuppressWarnings("unchecked")
        private Registry<T> target() {
            return (Registry<T>) delegate(name);
        }

        @Override
        public ResourceLocation key() {
            return target().key();
        }

        @Override
        public Registration<T> register(ResourceLocation id, Supplier<? extends T> factory) {
            checkOpen("unknown", id, LOG);
            return target().register(id, factory);
        }

        @Override
        public Optional<T> get(ResourceLocation id) {
            return target().get(id);
        }

        @Override
        public boolean contains(ResourceLocation id) {
            return target().contains(id);
        }

        @Override
        public List<ResourceLocation> ids() {
            return target().ids();
        }

        @Override
        public Stream<T> stream() {
            return target().stream();
        }

        @Override
        public int size() {
            return target().size();
        }
    }
}
