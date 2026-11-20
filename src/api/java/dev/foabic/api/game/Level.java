package dev.foabic.api.game;

import dev.foabic.api.util.ResourceLocation;

import java.util.List;

/**
 * A loaded dimension.
 */
public interface Level {

    ResourceLocation dimension();

    Server server();

    /**
     * The dimension's default world clock time. Minecraft 26.x replaced the
     * old per-level game time with world clocks; this is the default clock.
     */
    long time();

    List<Player> players();

    <T> T unwrap(Class<T> type);
}
