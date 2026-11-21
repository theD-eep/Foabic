package dev.foabic.api.game;

import java.util.Optional;

public interface CommandSource {

    String name();

    boolean isPlayer();

    boolean hasPermission(int level);

    void sendMessage(String message);

    default Optional<Player> player() {
        return this instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    <T> T unwrap(Class<T> type);
}
