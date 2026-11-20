package dev.foabic.api.event;

import dev.foabic.api.game.Server;

import java.util.Objects;

/**
 * Fired at the end of a server tick, after levels have ticked.
 */
public record ServerTickEndEvent(Server server, long tick) {

    public ServerTickEndEvent {
        Objects.requireNonNull(server, "server");
        if (tick < 0) {
            throw new IllegalArgumentException("tick must be >= 0");
        }
    }
}
