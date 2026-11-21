package dev.foabic.api.registry;

import dev.foabic.api.util.ResourceLocation;

/**
 * A handle returned by {@link Registry#register(ResourceLocation, java.util.function.Supplier)}.
 *
 * <p>The value may be created immediately (Fabric) or during the platform's
 * registration callback (NeoForge). Call {@link #get()} from a later
 * registration factory or from a signal listener, not from the same
 * initialization statement that created a deferred object.
 */
public interface Registration<T> {

    ResourceLocation id();

    /** Returns the registered value, or throws if it has not been created yet. */
    T get();

    /** Whether the value has been created and is available. */
    boolean isPresent();
}
