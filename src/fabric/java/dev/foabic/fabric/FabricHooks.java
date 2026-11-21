package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.event.LevelTickEndEvent;
import dev.foabic.api.event.LevelTickEvent;
import dev.foabic.api.event.PlayerJoinEvent;
import dev.foabic.api.event.PlayerLeaveEvent;
import dev.foabic.api.event.ServerStartedEvent;
import dev.foabic.api.event.ServerStartingEvent;
import dev.foabic.api.event.ServerStoppedEvent;
import dev.foabic.api.event.ServerStoppingEvent;
import dev.foabic.api.event.ServerTickEndEvent;
import dev.foabic.api.event.ServerTickEvent;
import dev.foabic.fabric.game.FabricLevel;
import dev.foabic.fabric.game.FabricPlayer;
import dev.foabic.fabric.game.FabricServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FabricHooks {

    private static final System.Logger LOG = System.getLogger("Foabic/fabric");

    private static volatile FabricServer server;
    private static volatile long tick;

    private FabricHooks() {
    }

    public static FabricServer server() {
        return server;
    }

    static void serverStarting(MinecraftServer minecraft) {
        FabricServer wrapped = new FabricServer(minecraft);
        server = wrapped;
        tick = 0;
        fire(new ServerStartingEvent(wrapped));
    }

    static void serverStarted(MinecraftServer minecraft) {
        FabricServer wrapped = ensure(minecraft);
        fire(new ServerStartedEvent(wrapped));
    }

    static void startTick(MinecraftServer minecraft) {
        FabricServer wrapped = ensure(minecraft);
        tick++;
        wrapped.setTick(tick);
        try {
            Foabic.scheduler().tick(tick);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "scheduler tick failed", failure);
        }
        fire(new ServerTickEvent(wrapped, tick));
    }

    static void endTick(MinecraftServer minecraft) {
        FabricServer wrapped = server;
        if (wrapped != null) fire(new ServerTickEndEvent(wrapped, tick));
    }

    static void levelStart(ServerLevel level) {
        FabricServer wrapped = server;
        if (wrapped != null) {
            fire(new LevelTickEvent(wrapped, new FabricLevel(level, wrapped), tick));
        }
    }

    static void levelEnd(ServerLevel level) {
        FabricServer wrapped = server;
        if (wrapped != null) {
            fire(new LevelTickEndEvent(wrapped, new FabricLevel(level, wrapped), tick));
        }
    }

    static void join(ServerPlayer player) {
        FabricServer wrapped = server;
        if (wrapped != null) fire(new PlayerJoinEvent(new FabricPlayer(player, wrapped)));
    }

    static void leave(ServerPlayer player) {
        FabricServer wrapped = server;
        if (wrapped != null) fire(new PlayerLeaveEvent(new FabricPlayer(player, wrapped)));
    }

    static void stopping(MinecraftServer minecraft) {
        FabricServer wrapped = server;
        if (wrapped != null) fire(new ServerStoppingEvent(wrapped));
    }

    static void stopped(MinecraftServer minecraft) {
        FabricServer wrapped = server;
        if (wrapped != null) fire(new ServerStoppedEvent(wrapped));
        server = null;
        tick = 0;
    }

    private static FabricServer ensure(MinecraftServer minecraft) {
        FabricServer wrapped = server;
        if (wrapped == null) {
            wrapped = new FabricServer(minecraft);
            server = wrapped;
        }
        return wrapped;
    }

    private static void fire(Object event) {
        try {
            Foabic.events().post(event);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "Foabic event dispatch failed", failure);
        }
    }
}
