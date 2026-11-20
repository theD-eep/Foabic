package dev.foabic.api.event;

import dev.foabic.api.game.Player;

import java.util.Objects;

/**
 * Fired after a player has been placed into the world and is visible to other
 * players. The player is not necessarily fully caught up with chunks yet.
 */
public record PlayerJoinEvent(Player player) {

    public PlayerJoinEvent {
        Objects.requireNonNull(player, "player");
    }
}
