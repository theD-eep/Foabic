package dev.foabic.api.event;

/**
 * A handle for removing an event listener. Closing the same subscription more
 * than once has no effect.
 */
@FunctionalInterface
public interface EventSubscription extends AutoCloseable {

    @Override
    void close();
}
