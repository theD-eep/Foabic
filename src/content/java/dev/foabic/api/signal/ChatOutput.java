package dev.foabic.api.signal;

/** A platform-neutral sink for sending an extra chat line. */
@FunctionalInterface
public interface ChatOutput {

    /**
     * Sends a server chat line visible to all players, displayed with
     * {@code <name> message}.
     */
    void sendAs(String name, String message);
}
