package dev.foabic.api.game;

import dev.foabic.api.util.ResourceLocation;

import java.util.List;

public interface Level {

    ResourceLocation dimension();

    Server server();

    long time();

    List<Player> players();

    <T> T unwrap(Class<T> type);
}
