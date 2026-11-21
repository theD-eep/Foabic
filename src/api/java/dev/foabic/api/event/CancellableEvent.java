package dev.foabic.api.event;

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
