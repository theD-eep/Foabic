package dev.foabic.neoforge;

import dev.foabic.api.Foabic;
import dev.foabic.api.signal.ChatMessageSignal;
import dev.foabic.api.signal.PlayerBreakBlockSignal;
import dev.foabic.api.signal.PlayerInteractBlockSignal;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.signal.ServerTickSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge implementation of the Foabic signal and interaction bridges. */
final class NeoForgeSignals {

    private static final System.Logger LOG = System.getLogger("Foabic/neoforge");

    private NeoForgeSignals() {
    }

    static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        Foabic.events().post(new ServerStartedSignal(
                FoabicNeoForge.PLATFORM, Foabic.MINECRAFT, Foabic.mods().size(),
                event.getServer()));
    }

    static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        dev.foabic.api.internal.FoabicInternal.onServerStopped();
        LOG.log(System.Logger.Level.INFO,
                "FOABIC_STOP_CLEAN platform=neoforge tick_listeners={0}",
                Foabic.events().hasListeners(ServerTickSignal.class));
    }

    static void onServerTick(ServerTickEvent.Post event) {
        Foabic.events().post(new ServerTickSignal(
                FoabicNeoForge.PLATFORM,
                event.getServer().getTickCount(),
                event.getServer().getPlayerCount()));
    }

    static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        ChatMessageSignal signal = new ChatMessageSignal(
                event.getUsername(),
                event.getRawText(),
                (name, text) -> player.level().getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal("<" + name + "> " + text), false));
        Foabic.events().post(signal);
    }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockState state = level.getBlockState(event.getPos());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return;

        Identifier dimension = level.dimension().identifier();
        ResourceLocation apiBlock = new ResourceLocation(blockId.getNamespace(), blockId.getPath());
        dev.foabic.api.util.BlockPos pos = new dev.foabic.api.util.BlockPos(
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());

        PlayerInteractBlockSignal signal = new PlayerInteractBlockSignal(
                serverPlayer.getName().getString(),
                dimension.toString(),
                apiBlock,
                pos,
                event.getHand() == InteractionHand.MAIN_HAND
                        ? PlayerInteractBlockSignal.Hand.MAIN_HAND
                        : PlayerInteractBlockSignal.Hand.OFF_HAND);

        Foabic.events().post(signal);
        bridgeLegacy(signal, serverPlayer, dimension, apiBlock, pos, event.getHand());

        switch (signal.result()) {
            case PASS -> {
                // Let vanilla continue.
            }
            case SUCCESS -> cancel(event, InteractionResult.SUCCESS);
            case CONSUME -> cancel(event, InteractionResult.CONSUME);
            case FAIL -> cancel(event, InteractionResult.FAIL);
        }
    }

    @SuppressWarnings("deprecation")
    private static void bridgeLegacy(PlayerInteractBlockSignal signal,
                                     ServerPlayer player,
                                     Identifier dimension,
                                     ResourceLocation block,
                                     dev.foabic.api.util.BlockPos pos,
                                     InteractionHand hand) {
        if (!Foabic.events().hasListeners(UseBlockSignal.class)) return;
        UseBlockSignal legacy = new UseBlockSignal(
                player.getName().getString(),
                dimension.toString(),
                block,
                pos,
                hand == InteractionHand.MAIN_HAND
                        ? UseBlockSignal.Hand.MAIN_HAND
                        : UseBlockSignal.Hand.OFF_HAND);
        legacy.setResult(UseBlockSignal.Result.valueOf(signal.result().name()));
        Foabic.events().post(legacy);
        signal.setResult(PlayerInteractBlockSignal.Result.valueOf(legacy.result().name()));
    }

    static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        BlockState state = level.getBlockState(event.getPos());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return;

        PlayerBreakBlockSignal signal = new PlayerBreakBlockSignal(
                serverPlayer.getName().getString(),
                level.dimension().identifier().toString(),
                new ResourceLocation(blockId.getNamespace(), blockId.getPath()),
                new dev.foabic.api.util.BlockPos(
                        event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()));
        Foabic.events().post(signal);
        if (signal.isCancelled()) {
            event.setCanceled(true);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
