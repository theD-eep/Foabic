package dev.foabic.fabric.game;

import dev.foabic.api.game.Player;
import dev.foabic.api.game.Server;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FabricServer implements Server {

    private final MinecraftServer server;
    private volatile long tick;

    public FabricServer(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
        this.tick = server.getTickCount();
    }

    public MinecraftServer raw() {
        return server;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    @Override
    public String name() {
        return server.getWorldData().getLevelName();
    }

    @Override
    public String motd() {
        return server.getMotd();
    }

    @Override
    public String version() {
        return server.getServerVersion();
    }

    @Override
    public boolean dedicated() {
        return server.isDedicatedServer();
    }

    @Override
    public long tick() {
        return tick;
    }

    @Override
    public int playerCount() {
        return server.getPlayerList().getPlayers().size();
    }

    @Override
    public List<Player> players() {
        List<ServerPlayer> raw = server.getPlayerList().getPlayers();
        List<Player> players = new ArrayList<>(raw.size());
        for (ServerPlayer player : raw) players.add(new FabricPlayer(player, this));
        return List.copyOf(players);
    }

    @Override
    public Path directory() {
        return server.getServerDirectory();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(server)) return type.cast(server);
        throw new ClassCastException("server is not a " + type.getName());
    }
}
