package dev.foabic.mc26.bridge;

import dev.foabic.api.game.Player;
import dev.foabic.api.game.Server;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

final class McPlayer implements Player {

    private final ServerPlayer player;
    private final McServer server;

    McPlayer(ServerPlayer player, McServer server) {
        this.player = Objects.requireNonNull(player, "player");
        this.server = Objects.requireNonNull(server, "server");
    }

    ServerPlayer raw() {
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
        return McPermissions.level(player.permissions());
    }

    @Override
    public boolean hasPermission(int level) {
        return McPermissions.has(player.permissions(), level);
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
        if (type.isInstance(player)) {
            return type.cast(player);
        }
        throw new ClassCastException("player is not a " + type.getName());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof McPlayer that && player.getUUID().equals(that.player.getUUID());
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
