package dev.foabic.api.event;

import dev.foabic.api.game.Server;
import java.util.Objects;

public record ServerTickEndEvent(Server server, long tick) {
    public ServerTickEndEvent {
        Objects.requireNonNull(server, "server");
        if (tick < 0) throw new IllegalArgumentException("tick must be >= 0");
    }
}
