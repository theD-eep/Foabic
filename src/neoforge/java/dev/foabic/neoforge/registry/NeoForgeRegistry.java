package dev.foabic.neoforge.registry;

import dev.foabic.api.registry.Registration;
import dev.foabic.api.registry.Registry;
import dev.foabic.api.registry.RegistryException;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A deferred view over a NeoForge registry.
 *
 * <p>NeoForge mod constructors run after the vanilla registries have been
 * frozen. Constructing a {@code Block} there fails because the block
 * constructor creates an intrusive holder. Foobic therefore stores factories
 * and evaluates them from {@code RegisterEvent}, where the target registry is
 * writable. No vanilla registry is read during mod construction.
 */
public final class NeoForgeRegistry<T> implements Registry<T> {

    private final ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey;
    private final net.minecraft.core.Registry<T> registry;
    private final ResourceLocation key;
    private final Map<ResourceLocation, Pending<T>> pending = new LinkedHashMap<>();
    private boolean flushed;

    public NeoForgeRegistry(ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey,
                            net.minecraft.core.Registry<T> registry) {
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey");
        this.registry = Objects.requireNonNull(registry, "registry");
        Identifier id = registry.key().identifier();
        this.key = new ResourceLocation(id.getNamespace(), id.getPath());
    }

    @Override
    public ResourceLocation key() {
        return key;
    }

    @Override
    public synchronized Registration<T> register(ResourceLocation id, Supplier<? extends T> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        if (flushed) {
            throw new RegistryException("registry " + key
                    + " has already been flushed; register content during mod initialization");
        }
        if (pending.containsKey(id)) {
            throw new RegistryException("duplicate id '" + id + "' in registry " + key);
        }
        Pending<T> registration = new Pending<>(id, factory);
        pending.put(id, registration);
        return registration;
    }

    /** Called by the adapter for each {@code RegisterEvent}. */
    public synchronized void flush(RegisterEventAccess event) {
        if (flushed || !event.registryKey().equals(registryKey)) return;
        for (Pending<T> registration : pending.values()) {
            event.register(registryKey, mcId(registration.id), () -> {
                T value = Objects.requireNonNull(registration.factory.get(),
                        "factory returned null for " + registration.id);
                registration.value = value;
                return value;
            });
        }
        flushed = true;
    }

    @Override
    public synchronized Optional<T> get(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        Pending<T> registration = pending.get(id);
        if (registration != null && registration.value != null) {
            return Optional.of(registration.value);
        }
        return flushed ? registry.getOptional(mcId(id)) : Optional.empty();
    }

    @Override
    public synchronized boolean contains(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        if (!flushed) return pending.containsKey(id);
        return registry.containsKey(mcId(id));
    }

    @Override
    public synchronized List<ResourceLocation> ids() {
        if (!flushed) return List.copyOf(pending.keySet());
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        registry.keySet().forEach(id -> ids.add(new ResourceLocation(id.getNamespace(), id.getPath())));
        return List.copyOf(ids);
    }

    @Override
    public synchronized Stream<T> stream() {
        if (!flushed) {
            return pending.values().stream()
                    .map(registration -> registration.value)
                    .filter(Objects::nonNull);
        }
        return registry.stream();
    }

    @Override
    public synchronized int size() {
        return flushed ? registry.keySet().size() : (int) pending.values().stream()
                .filter(registration -> registration.value != null).count();
    }

    private static Identifier mcId(ResourceLocation id) {
        return Identifier.fromNamespaceAndPath(id.namespace(), id.path());
    }

    private static final class Pending<T> implements Registration<T> {
        private final ResourceLocation id;
        private final Supplier<? extends T> factory;
        private volatile T value;

        private Pending(ResourceLocation id, Supplier<? extends T> factory) {
            this.id = id;
            this.factory = factory;
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        @Override
        public T get() {
            T current = value;
            if (current == null) {
                throw new IllegalStateException("registration '" + id
                        + "' has not been created yet; the platform registration event has not run");
            }
            return current;
        }

        @Override
        public boolean isPresent() {
            return value != null;
        }
    }

    /** Small seam so the queue can be tested without a live NeoForge event. */
    public interface RegisterEventAccess {
        ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey();

        <V> void register(ResourceKey<? extends net.minecraft.core.Registry<V>> key, Identifier id,
                          Supplier<V> value);
    }
}
