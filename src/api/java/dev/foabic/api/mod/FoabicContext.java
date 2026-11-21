package dev.foabic.api.mod;

import dev.foabic.api.config.ConfigManager;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.event.FoabicSubscription;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * The platform-neutral part of a mod context.
 *
 * <p>The Minecraft-facing content methods live on {@link ModContext} in the
 * content module. Keeping this interface free of Minecraft types is what
 * allows the core API jar to stay under 15 KiB.
 */
public interface FoabicContext {

    ModContainer mod();

    EventBus events();

    /** {@code "fabric"} or {@code "neoforge"}. */
    String platform();

    System.Logger logger();

    /** Per-mod JSON configuration access. */
    ConfigManager config();

    /**
     * Subscribes to a signal and returns a removable handle. The subscription
     * is cleared automatically when the server stops.
     */
    default <S> FoabicSubscription connect(Class<S> signalType, Consumer<S> listener) {
        Objects.requireNonNull(signalType, "signalType");
        Objects.requireNonNull(listener, "listener");
        return events().subscribe(signalType, listener::accept);
    }
}
