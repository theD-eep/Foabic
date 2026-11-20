package dev.foabic.api.game;

import java.util.Optional;

/**
 * Anything that can run a command: a player, the server console, or a command
 * block. The permission scale follows vanilla's historic 0-4 levels.
 */
public interface CommandSource {

    String name();

    boolean isPlayer();

    /**
     * @param level a vanilla-style permission level, normally 0 through 4
     */
    boolean hasPermission(int level);

    void sendMessage(String message);

    default Optional<Player> player() {
        return this instanceof Player player ? Optional.of(player) : Optional.empty();
    }

    /**
     * Returns the underlying platform object when it is assignable to
     * {@code type}, otherwise throws {@link ClassCastException}.
     */
    <T> T unwrap(Class<T> type);
}
