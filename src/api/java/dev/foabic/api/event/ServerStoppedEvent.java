package dev.foabic.api.event;

import dev.foabic.api.game.Server;
import java.util.Objects;

public record ServerStoppedEvent(Server server) {
    public ServerStoppedEvent {
        Objects.requireNonNull(server, "server");
    }
}
