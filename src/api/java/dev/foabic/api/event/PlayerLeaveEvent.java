package dev.foabic.api.event;

import dev.foabic.api.game.Player;
import java.util.Objects;

public record PlayerLeaveEvent(Player player) {
    public PlayerLeaveEvent {
        Objects.requireNonNull(player, "player");
    }
}
