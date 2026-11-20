package dev.foabic.mc26.bridge;

import dev.foabic.api.game.Level;
import dev.foabic.api.game.Player;
import dev.foabic.api.game.Server;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class McLevel implements Level {

    private final ServerLevel level;
    private final McServer server;

    McLevel(ServerLevel level, McServer server) {
        this.level = Objects.requireNonNull(level, "level");
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public ResourceLocation dimension() {
        Identifier id = level.dimension().identifier();
        return new ResourceLocation(id.getNamespace(), id.getPath());
    }

    @Override
    public Server server() {
        return server;
    }

    @Override
    public long time() {
        return level.getDefaultClockTime();
    }

    @Override
    public List<Player> players() {
        List<ServerPlayer> raw = level.players();
        List<Player> players = new ArrayList<>(raw.size());
        for (ServerPlayer player : raw) {
            players.add(new McPlayer(player, server));
        }
        return List.copyOf(players);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(level)) {
            return type.cast(level);
        }
        throw new ClassCastException("level is not a " + type.getName());
    }

    @Override
    public String toString() {
        return "McLevel[" + dimension() + "]";
    }
}
