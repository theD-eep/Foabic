package dev.foabic.fabric;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import dev.foabic.fabric.command.FabricCommands;

final class FabricEvents {

    private static boolean registered;

    private FabricEvents() {
    }

    static synchronized void register() {
        if (registered) return;
        registered = true;

        ServerLifecycleEvents.SERVER_STARTING.register(FabricHooks::serverStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(FabricHooks::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(FabricHooks::stopping);
        ServerLifecycleEvents.SERVER_STOPPED.register(FabricHooks::stopped);

        ServerTickEvents.START_SERVER_TICK.register(FabricHooks::startTick);
        ServerTickEvents.END_SERVER_TICK.register(FabricHooks::endTick);
        ServerTickEvents.START_LEVEL_TICK.register(FabricHooks::levelStart);
        ServerTickEvents.END_LEVEL_TICK.register(FabricHooks::levelEnd);

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> FabricHooks.join(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> FabricHooks.leave(handler.player));

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> FabricCommands.register(dispatcher));

        UseBlockCallback.EVENT.register(FabricSignals::useBlock);
    }
}
