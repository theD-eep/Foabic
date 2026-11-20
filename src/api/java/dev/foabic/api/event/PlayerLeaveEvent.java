package dev.foabic.api.event;

import dev.foabic.api.game.Player;

import java.util.Objects;

/**
 * Fired when a player is removed from the player list, before their entity is
 * fully discarded.
 */
public record PlayerLeaveEvent(Player player) {

    public PlayerLeaveEvent {
        Objects.requireNonNull(player, "player");
    }
}
