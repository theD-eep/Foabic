package dev.foabic.api.event;

import dev.foabic.api.game.Level;
import dev.foabic.api.game.Server;
import java.util.Objects;

public record LevelTickEndEvent(Server server, Level level, long tick) {
    public LevelTickEndEvent {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(level, "level");
        if (tick < 0) throw new IllegalArgumentException("tick must be >= 0");
    }
}
