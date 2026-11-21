package dev.foabic.api.event;

import dev.foabic.api.game.Server;
import java.util.Objects;

public record ServerStartedEvent(Server server) {
    public ServerStartedEvent {
        Objects.requireNonNull(server, "server");
    }
}
