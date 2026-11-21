package dev.foabic.fabric.game;

import dev.foabic.api.game.Player;
import dev.foabic.api.game.Server;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public final class FabricPlayer implements Player {

    private final ServerPlayer player;
    private final FabricServer server;

    public FabricPlayer(ServerPlayer player, FabricServer server) {
        this.player = Objects.requireNonNull(player, "player");
        this.server = Objects.requireNonNull(server, "server");
    }

    public ServerPlayer raw() {
        return player;
    }

    @Override
    public UUID id() {
        return player.getUUID();
    }

    @Override
    public String name() {
        return player.getGameProfile().name();
    }

    @Override
    public Server server() {
        return server;
    }

    @Override
    public int permissionLevel() {
        return FabricPermissions.level(player.permissions());
    }

    @Override
    public boolean hasPermission(int level) {
        return FabricPermissions.has(player.permissions(), level);
    }

    @Override
    public void sendMessage(String message) {
        player.sendSystemMessage(Component.literal(message));
    }

    @Override
    public void sendActionBar(String message) {
        player.sendOverlayMessage(Component.literal(message));
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(player)) return type.cast(player);
        throw new ClassCastException("player is not a " + type.getName());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FabricPlayer that && player.getUUID().equals(that.player.getUUID());
    }

    @Override
    public int hashCode() {
        return player.getUUID().hashCode();
    }

    @Override
    public String toString() {
        return name();
    }
}
