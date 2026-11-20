package dev.foabic.api.event;

import dev.foabic.api.game.Server;

import java.util.Objects;

/**
 * Fired when the server starts its shutdown sequence.
 */
public record ServerStoppingEvent(Server server) {

    public ServerStoppingEvent {
        Objects.requireNonNull(server, "server");
    }
}
