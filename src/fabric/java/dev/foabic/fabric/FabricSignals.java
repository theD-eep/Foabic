package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.fabric.game.FabricLevel;
import dev.foabic.fabric.game.FabricPlayer;
import dev.foabic.fabric.game.FabricServer;
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

/** Translates the one Fabric interaction hook used by Foabic 0.2. */
final class FabricSignals {

    private FabricSignals() {
    }

    static InteractionResult useBlock(
            Player rawPlayer,
            Level rawLevel,
            InteractionHand rawHand,
            BlockHitResult hit
    ) {
        if (rawLevel.isClientSide() || !(rawPlayer instanceof ServerPlayer player)
                || !(rawLevel instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }
        FabricServer server = FabricHooks.server();
        if (server == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(hit.getBlockPos());
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (id == null) return InteractionResult.PASS;

        UseBlockSignal signal = new UseBlockSignal(
                new FabricPlayer(player, server),
                new FabricLevel(level, server),
                new ResourceLocation(id.getNamespace(), id.getPath()),
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
