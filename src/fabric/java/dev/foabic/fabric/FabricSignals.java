package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The only Fabric-specific signal bridge in Foabic 1.0. */
final class FabricSignals {

    private FabricSignals() {
    }

    static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                Foabic.events().post(new ServerStartedSignal(
                        FoabicFabric.PLATFORM, Foabic.MINECRAFT, Foabic.mods().size())));

        UseBlockCallback.EVENT.register(FabricSignals::useBlock);
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
        UseBlockSignal signal = new UseBlockSignal(
                player.getName().getString(),
                dimension.toString(),
                new ResourceLocation(blockId.getNamespace(), blockId.getPath()),
                new BlockPos(hit.getBlockPos().getX(),
                        hit.getBlockPos().getY(), hit.getBlockPos().getZ()),
                rawHand == InteractionHand.MAIN_HAND
                        ? UseBlockSignal.Hand.MAIN_HAND
                        : UseBlockSignal.Hand.OFF_HAND);

        Foabic.events().post(signal);
        return switch (signal.result()) {
            case PASS -> InteractionResult.PASS;
            case SUCCESS -> InteractionResult.SUCCESS;
            case CONSUME -> InteractionResult.CONSUME;
            case FAIL -> InteractionResult.FAIL;
        };
    }
}
