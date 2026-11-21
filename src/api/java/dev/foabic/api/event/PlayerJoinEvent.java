package dev.foabic.api.event;

import dev.foabic.api.game.Player;
import java.util.Objects;

public record PlayerJoinEvent(Player player) {
    public PlayerJoinEvent {
        Objects.requireNonNull(player, "player");
    }
}
