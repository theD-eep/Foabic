package dev.foabic.api.game;

import java.nio.file.Path;
import java.util.List;

/**
 * A read-only view of the running Minecraft server.
 *
 * <p>This interface intentionally exposes only stable, useful information. Use
 * {@link #unwrap(Class)} when a mod needs the full platform object.
 */
public interface Server {

    String name();

    String motd();

    String version();

    boolean dedicated();

    /**
     * The current server tick, starting at 1 for the first tick.
     */
    long tick();

    int playerCount();

    List<Player> players();

    Path directory();

    <T> T unwrap(Class<T> type);
}
