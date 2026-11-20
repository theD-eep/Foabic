package dev.foabic.api.event;

import dev.foabic.api.game.Server;

import java.util.Objects;

/**
 * Fired at the beginning of a server tick, after scheduled tasks for that tick
 * have run.
 *
 * @param tick the one-based server tick number
 */
public record ServerTickEvent(Server server, long tick) {

    public ServerTickEvent {
        Objects.requireNonNull(server, "server");
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0");
        }
    }
}
