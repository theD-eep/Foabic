package dev.foabic.api.event;

public interface Cancellable {
    void cancel();
    boolean isCancelled();
}
