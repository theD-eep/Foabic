package dev.foabic.mc26.bridge;

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
import dev.foabic.mc26.spi.HookBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Translates raw Minecraft objects into Foabic events. This class lives next to
 * Minecraft's own class loader so its direct type references resolve in both
 * flat-classpath and vanilla-bundler launches.
 */
public final class McBridge implements HookBridge {

    private static final System.Logger LOG = System.getLogger("Foabic/mc26");

    private final Set<String> registeredCommands = new HashSet<>();
    private McServer server;
    private long tick;
    private boolean started;
    private boolean stopping;

    public McBridge() {
    }

    McServer server() {
        return server;
    }

    @Override
    public void serverStarting(Object raw) {
        if (!(raw instanceof MinecraftServer minecraft)) {
            return;
        }
        McServer wrapped = new McServer(minecraft);
        server = wrapped;
        tick = 0;
        started = false;
        stopping = false;
        registeredCommands.clear();
        fire(new ServerStartingEvent(wrapped));
    }

    @Override
    public void serverTickStart(Object raw) {
        McServer wrapped = server;
        if (wrapped == null) {
            if (raw instanceof MinecraftServer minecraft) {
                wrapped = new McServer(minecraft);
                server = wrapped;
            } else {
                return;
            }
        }

        tick++;
        wrapped.setTick(tick);
        if (!started) {
            started = true;
            flushCommands(wrapped);
            fire(new ServerStartedEvent(wrapped));
            // Let initializers that register commands in the started event join
            // the same first-tick registration pass.
            flushCommands(wrapped);
        }

        try {
            Foabic.scheduler().tick(tick);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "scheduler tick failed", failure);
        }
        fire(new ServerTickEvent(wrapped, tick));
    }

    @Override
    public void serverTickEnd(Object raw) {
        McServer wrapped = server;
        if (wrapped != null) {
            fire(new ServerTickEndEvent(wrapped, tick));
        }
    }

    @Override
    public void serverStopping(Object raw) {
        McServer wrapped = server;
        if (wrapped == null || stopping) {
            return;
        }
        stopping = true;
        fire(new ServerStoppingEvent(wrapped));
    }

    @Override
    public void serverStopped(Object raw) {
        McServer wrapped = server;
        if (wrapped != null) {
            fire(new ServerStoppedEvent(wrapped));
        }
        server = null;
        started = false;
        stopping = false;
        tick = 0;
    }

    @Override
    public void levelTickStart(Object raw) {
        McServer wrapped = server;
        if (wrapped != null && raw instanceof ServerLevel level) {
            fire(new LevelTickEvent(wrapped, new McLevel(level, wrapped), tick));
        }
    }

    @Override
    public void levelTickEnd(Object raw) {
        McServer wrapped = server;
        if (wrapped != null && raw instanceof ServerLevel level) {
            fire(new LevelTickEndEvent(wrapped, new McLevel(level, wrapped), tick));
        }
    }

    @Override
    public void playerJoin(Object raw) {
        McServer wrapped = server;
        if (wrapped != null && raw instanceof ServerPlayer player) {
            fire(new PlayerJoinEvent(new McPlayer(player, wrapped)));
        }
    }

    @Override
    public void playerLeave(Object raw) {
        McServer wrapped = server;
        if (wrapped != null && raw instanceof ServerPlayer player) {
            fire(new PlayerLeaveEvent(new McPlayer(player, wrapped)));
        }
    }

    private void flushCommands(McServer wrapped) {
        try {
            int added = McCommands.register(
                    wrapped.raw().getCommands().getDispatcher(),
                    Foabic.commands().roots(),
                    wrapped,
                    registeredCommands);
            if (added > 0) {
                LOG.log(System.Logger.Level.INFO, "registered {0} command(s)", added);
            }
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "command registration failed", failure);
        }
    }

    private static void fire(Object event) {
        try {
            Foabic.events().post(event);
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "event dispatch failed", failure);
        }
    }
}
