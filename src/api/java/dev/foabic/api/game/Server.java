package dev.foabic.api.game;

import java.nio.file.Path;
import java.util.List;

public interface Server {

    String name();

    String motd();

    String version();

    boolean dedicated();

    long tick();

    int playerCount();

    List<Player> players();

    Path directory();

    <T> T unwrap(Class<T> type);
}
