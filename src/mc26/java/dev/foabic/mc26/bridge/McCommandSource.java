package dev.foabic.mc26.bridge;

import dev.foabic.api.game.CommandSource;
import dev.foabic.api.game.Player;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

final class McCommandSource implements CommandSource {

    private final CommandSourceStack source;
    private final McServer server;
    private final ServerPlayer player;

    McCommandSource(CommandSourceStack source, McServer server) {
        this.source = Objects.requireNonNull(source, "source");
        this.server = Objects.requireNonNull(server, "server");
        this.player = source.getPlayer();
    }

    @Override
    public String name() {
        return source.getTextName();
    }

    @Override
    public boolean isPlayer() {
        return player != null;
    }

    @Override
    public boolean hasPermission(int level) {
        return McPermissions.has(source.permissions(), level);
    }

    @Override
    public void sendMessage(String message) {
        source.sendSystemMessage(Component.literal(message));
    }

    @Override
    public Optional<Player> player() {
        return player == null ? Optional.empty() : Optional.of(new McPlayer(player, server));
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(source)) {
            return type.cast(source);
        }
        if (player != null && type.isInstance(player)) {
            return type.cast(player);
        }
        throw new ClassCastException("command source is not a " + type.getName());
    }

    @Override
    public String toString() {
        return name();
    }
}
