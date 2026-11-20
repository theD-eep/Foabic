package dev.foabic.api.event;

import dev.foabic.api.game.Server;

import java.util.Objects;

/**
 * Fired once when the dedicated server begins its startup routine, before the
 * first tick. The server is not yet accepting players.
 */
public record ServerStartingEvent(Server server) {

    public ServerStartingEvent {
        Objects.requireNonNull(server, "server");
    }
}
