package dev.foabic.api.event;

/**
 * Implemented by events that can stop later listeners from running.
 *
 * <p>Cancellation is cooperative: the event bus stops dispatching, but the
 * game-side action is only prevented when the adapter checks the event after
 * posting. Events in Foabic 0.1 are notifications, not vetoable actions.
 */
public interface Cancellable {

    void cancel();

    boolean isCancelled();
}
