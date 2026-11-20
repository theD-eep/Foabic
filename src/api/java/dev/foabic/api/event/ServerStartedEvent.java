package dev.foabic.api.event;

import dev.foabic.api.game.Server;

import java.util.Objects;

/**
 * Fired on the first server tick, immediately before {@link ServerTickEvent}.
 * Commands have already been registered by the time this event is posted.
 */
public record ServerStartedEvent(Server server) {

    public ServerStartedEvent {
        Objects.requireNonNull(server, "server");
    }
}
