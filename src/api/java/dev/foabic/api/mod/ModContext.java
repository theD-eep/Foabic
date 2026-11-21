package dev.foabic.api.mod;

import dev.foabic.api.command.CommandRegistry;
import dev.foabic.api.config.Config;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.EventSubscription;
import dev.foabic.api.registry.Registry;
import dev.foabic.api.scheduler.Scheduler;
import dev.foabic.api.util.ResourceLocation;

import java.util.function.Consumer;

public interface ModContext {

    ModContainer mod();

    EventBus events();

    Scheduler scheduler();

    CommandRegistry commands();

    Environment environment();

    System.Logger logger();

    default Config config() {
        return mod().config();
    }

    default ModResources resources() {
        return mod().resources();
    }

    /**
     * Registers content through a platform registry view. On Fabric, use
     * {@code FabricRegistries.BLOCKS}, {@code ITEMS}, etc.
     */
    default <T> T submit(Registry<T> registry, ResourceLocation id, T value) {
        return registry.register(id, value);
    }

    /** Subscribes to a signal/event with a concise name. */
    default <S> EventSubscription connect(Class<S> signalType, Consumer<S> listener) {
        return events().subscribe(signalType, listener::accept);
    }
}
