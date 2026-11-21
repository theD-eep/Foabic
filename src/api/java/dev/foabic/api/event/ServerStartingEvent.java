package dev.foabic.api.event;

import dev.foabic.api.game.Server;
import java.util.Objects;

public record ServerStartingEvent(Server server) {
    public ServerStartingEvent {
        Objects.requireNonNull(server, "server");
    }
}
