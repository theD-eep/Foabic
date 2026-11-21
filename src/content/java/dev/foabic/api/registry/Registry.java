package dev.foabic.api.registry;

import dev.foabic.api.util.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A platform registry view.
 *
 * <p>Registrations are expressed as factories, not pre-built values. Fabric
 * evaluates a factory immediately during mod initialization. NeoForge
 * evaluates it inside {@code RegisterEvent}, which is the only point where
 * constructing a {@code Block} is legal in 26.3. This is why the common API
 * does not accept an already-built block.
 *
 * @param <T> the registered element type
 */
public interface Registry<T> {

    ResourceLocation key();

    Registration<T> register(ResourceLocation id, Supplier<? extends T> factory);

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
