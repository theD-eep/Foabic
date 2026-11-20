package dev.foabic.api.game;

import java.util.Optional;
import java.util.UUID;

/**
 * A connected player.
 */
public interface Player extends CommandSource {

    UUID id();

    Server server();

    int permissionLevel();

    void sendActionBar(String message);

    @Override
    default boolean isPlayer() {
        return true;
    }

    @Override
    default Optional<Player> player() {
        return Optional.of(this);
    }
}
