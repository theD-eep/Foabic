package dev.foabic.api.registry;

import dev.foabic.api.util.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A platform registry view.
 *
 * <p>Registrations must happen during mod initialization, before Minecraft
 * freezes its registries. The Fabric adapter wraps the corresponding vanilla
 * registry and reports a clear error when registration is too late.
 *
 * @param <T> the registered element type
 */
public interface Registry<T> {

    ResourceLocation key();

    <V extends T> V register(ResourceLocation id, V value);

    Optional<T> get(ResourceLocation id);

    default T getOrThrow(ResourceLocation id) {
        return get(id).orElseThrow(() -> new RegistryException(
                "no element '" + id + "' in registry " + key()));
    }

    boolean contains(ResourceLocation id);

    List<ResourceLocation> ids();

    Stream<T> stream();

    int size();
}
