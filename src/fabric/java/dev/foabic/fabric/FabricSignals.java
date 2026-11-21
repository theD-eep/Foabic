package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.signal.ChatMessageSignal;
import dev.foabic.api.signal.PlayerBreakBlockSignal;
import dev.foabic.api.signal.PlayerInteractBlockSignal;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.signal.ServerTickSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.ResourceLocation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Fabric implementation of the Foabic signal and interaction bridges. */
final class FabricSignals {

    private FabricSignals() {
    }

    static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                Foabic.events().post(new ServerStartedSignal(
                        FoabicFabric.PLATFORM, Foabic.MINECRAFT, Foabic.mods().size())));

        ServerTickEvents.END_SERVER_TICK.register(server ->
                Foabic.events().post(new ServerTickSignal(
                        FoabicFabric.PLATFORM, server.getTickCount(), server.getPlayerCount())));

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, bound) -> {
            ChatMessageSignal signal = new ChatMessageSignal(
                    sender.getName().getString(),
                    message.signedContent(),
                    (name, text) -> {
                        if (sender.level().getServer() != null) {
                            sender.level().getServer().getPlayerList().broadcastSystemMessage(
                                    Component.literal("<" + name + "> " + text), false);
                        }
                    });
            Foabic.events().post(signal);
        });

        UseBlockCallback.EVENT.register(FabricSignals::useBlock);
        PlayerBlockBreakEvents.BEFORE.register(FabricSignals::beforeBreak);
    }

    private static InteractionResult useBlock(
            Player rawPlayer,
            Level rawLevel,
            InteractionHand rawHand,
            BlockHitResult hit
    ) {
        if (rawLevel.isClientSide()
                || !(rawPlayer instanceof ServerPlayer player)
                || !(rawLevel instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(hit.getBlockPos());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return InteractionResult.PASS;

        Identifier dimension = level.dimension().identifier();
        dev.foabic.api.util.BlockPos apiPos = new dev.foabic.api.util.BlockPos(
                hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
        ResourceLocation apiBlock = new ResourceLocation(blockId.getNamespace(), blockId.getPath());

        PlayerInteractBlockSignal signal = new PlayerInteractBlockSignal(
                player.getName().getString(),
                dimension.toString(),
                apiBlock,
                apiPos,
                rawHand == InteractionHand.MAIN_HAND
                        ? PlayerInteractBlockSignal.Hand.MAIN_HAND
                        : PlayerInteractBlockSignal.Hand.OFF_HAND);

        Foabic.events().post(signal);
        bridgeLegacy(signal, player, dimension, apiBlock, apiPos, rawHand);
        return toInteraction(signal.result());
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

    private static InteractionResult toInteraction(PlayerInteractBlockSignal.Result result) {
        return switch (result) {
            case PASS -> InteractionResult.PASS;
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            case FAIL -> InteractionResult.FAIL;
        };
    }

    private static boolean beforeBreak(Level level, Player player, BlockPos pos,
                                       BlockState state, BlockEntity blockEntity) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return true;

        PlayerBreakBlockSignal signal = new PlayerBreakBlockSignal(
                serverPlayer.getName().getString(),
                level.dimension().identifier().toString(),
                new ResourceLocation(blockId.getNamespace(), blockId.getPath()),
                new dev.foabic.api.util.BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        Foabic.events().post(signal);
        return !signal.isCancelled();
    }
}
