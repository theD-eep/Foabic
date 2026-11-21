package dev.foabic.fabric.registry;

import dev.foabic.api.registry.Registration;
import dev.foabic.api.registry.Registry;
import dev.foabic.api.registry.RegistryException;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class FabricRegistry<T> implements Registry<T> {

    private final net.minecraft.core.Registry<T> registry;
    private final ResourceLocation key;

    FabricRegistry(net.minecraft.core.Registry<T> registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        Identifier id = registry.key().identifier();
        this.key = new ResourceLocation(id.getNamespace(), id.getPath());
    }

    @Override
    public ResourceLocation key() {
        return key;
    }

    @Override
    public Registration<T> register(ResourceLocation id, Supplier<? extends T> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        Identifier mcId = Identifier.fromNamespaceAndPath(id.namespace(), id.path());
        if (registry.containsKey(mcId)) {
            throw new RegistryException("duplicate id '" + id + "' in registry " + key);
        }
        T value;
        try {
            value = Objects.requireNonNull(factory.get(), "factory returned null for " + id);
            net.minecraft.core.Registry.register(registry, mcId, value);
        } catch (RegistryException e) {
            throw e;
        } catch (IllegalStateException e) {
            throw new RegistryException(
                    "registry " + key + " is frozen; register content during mod initialization", e);
        }
        return new FabricRegistration<>(id, value);
    }

    @Override
    public Optional<T> get(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return registry.getOptional(Identifier.fromNamespaceAndPath(id.namespace(), id.path()));
    }

    @Override
    public boolean contains(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return registry.containsKey(Identifier.fromNamespaceAndPath(id.namespace(), id.path()));
    }

    @Override
    public List<ResourceLocation> ids() {
        return registry.keySet().stream()
                .map(id -> new ResourceLocation(id.getNamespace(), id.getPath()))
                .toList();
    }

    @Override
    public Stream<T> stream() {
        return registry.stream();
    }

    @Override
    public int size() {
        return registry.keySet().size();
    }

    private record FabricRegistration<V>(ResourceLocation id, V value) implements Registration<V> {
        @Override
        public V get() {
            return value;
        }

        @Override
        public boolean isPresent() {
            return true;
        }
    }
}
