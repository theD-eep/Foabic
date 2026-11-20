package dev.foabic.api.event;

/**
 * Convenience base class for cancellable events.
 */
public abstract class CancellableEvent implements Cancellable {

    private volatile boolean cancelled;

    @Override
    public final void cancel() {
        cancelled = true;
    }

    @Override
    public final boolean isCancelled() {
        return cancelled;
    }
}
