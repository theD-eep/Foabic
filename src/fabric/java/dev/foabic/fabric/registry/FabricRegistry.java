package dev.foabic.fabric.registry;

import dev.foabic.api.registry.Registry;
import dev.foabic.api.registry.RegistryException;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    public <V extends T> V register(ResourceLocation id, V value) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(value, "value");
        Identifier mcId = Identifier.fromNamespaceAndPath(id.namespace(), id.path());
        if (registry.containsKey(mcId)) {
            throw new RegistryException("duplicate id '" + id + "' in registry " + key);
        }
        try {
            return net.minecraft.core.Registry.register(registry, mcId, value);
        } catch (IllegalStateException e) {
            throw new RegistryException(
                    "registry " + key + " is frozen; register content during mod initialization", e);
        }
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
}
