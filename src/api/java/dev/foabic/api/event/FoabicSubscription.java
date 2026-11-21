package dev.foabic.api.event;

/**
 * A removable signal registration.
 *
 * <p>{@link EventBus#subscribe} and
 * {@link dev.foabic.api.mod.FoabicContext#connect} return this handle. Call
 * {@link #unsubscribe()} when the listener is no longer needed; the adapter
 * also clears the session automatically when the server stops.
 */
@FunctionalInterface
public interface FoabicSubscription extends AutoCloseable {

    /** Removes the listener. Calling this more than once is harmless. */
    void unsubscribe();

    @Override
    default void close() {
        unsubscribe();
    }
}
