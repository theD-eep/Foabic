package dev.foabic.api.mod;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.EventSubscription;
import dev.foabic.api.registry.Registration;
import dev.foabic.api.registry.Registry;
import dev.foabic.api.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** The per-mod handle passed to {@link ModInitializer#onInitialize(ModContext)}. */
public interface ModContext {

    ModContainer mod();

    EventBus events();

    Environment environment();

    /** {@code "fabric"} or {@code "neoforge"}. */
    String platform();

    System.Logger logger();

    /**
     * Registers one object in a Foabic registry view.
     *
     * <p>Use {@link dev.foabic.api.registry.FoabicRegistries#BLOCKS} or
     * {@link dev.foabic.api.registry.FoabicRegistries#ITEMS}. The factory is
     * evaluated during the platform's registration window. Pass
     * {@link Registration#get()} to a later factory when one object needs
     * another, as in {@code new BlockItem(block.get(), ...)}.
     */
    default <T> Registration<T> submit(Registry<T> registry, ResourceLocation id,
                                       Supplier<? extends T> factory) {
        return registry.register(id, factory);
    }

    /** Subscribes to a signal using the shared synchronous bus. */
    default <S> EventSubscription connect(Class<S> signalType, Consumer<S> listener) {
        return events().subscribe(signalType, listener::accept);
    }
}
