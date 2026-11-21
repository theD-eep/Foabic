package dev.foabic.fabric.command;

import dev.foabic.api.game.CommandSource;
import dev.foabic.api.game.Player;
import dev.foabic.fabric.FabricHooks;
import dev.foabic.fabric.game.FabricPermissions;
import dev.foabic.fabric.game.FabricPlayer;
import dev.foabic.fabric.game.FabricServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

public final class FabricCommandSource implements CommandSource {

    private final CommandSourceStack source;
    private final FabricServer server;
    private final ServerPlayer player;

    public FabricCommandSource(CommandSourceStack source) {
        this.source = Objects.requireNonNull(source, "source");
        FabricServer current = FabricHooks.server();
        if (current == null) {
            MinecraftServer minecraft = source.getServer();
            current = new FabricServer(minecraft);
        }
        this.server = current;
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
        return FabricPermissions.has(source.permissions(), level);
    }

    @Override
    public void sendMessage(String message) {
        source.sendSystemMessage(Component.literal(message));
    }

    @Override
    public Optional<Player> player() {
        return player == null ? Optional.empty() : Optional.of(new FabricPlayer(player, server));
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        Objects.requireNonNull(type, "type");
        if (type.isInstance(source)) return type.cast(source);
        if (player != null && type.isInstance(player)) return type.cast(player);
        throw new ClassCastException("command source is not a " + type.getName());
    }
}
