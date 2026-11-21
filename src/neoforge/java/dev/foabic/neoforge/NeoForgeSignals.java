package dev.foabic.neoforge;

import dev.foabic.api.Foabic;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** The only NeoForge-specific signal bridge in Foabic 1.0. */
final class NeoForgeSignals {

    private NeoForgeSignals() {
    }

    static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        Foabic.events().post(new ServerStartedSignal(
                FoabicNeoForge.PLATFORM, Foabic.MINECRAFT, Foabic.mods().size()));
    }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }

        BlockState state = level.getBlockState(event.getPos());
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) return;

        Identifier dimension = level.dimension().identifier();
        UseBlockSignal signal = new UseBlockSignal(
                player.getName().getString(),
                dimension.toString(),
                new ResourceLocation(blockId.getNamespace(), blockId.getPath()),
                new BlockPos(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()),
                event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? UseBlockSignal.Hand.MAIN_HAND
                        : UseBlockSignal.Hand.OFF_HAND);

        Foabic.events().post(signal);
        switch (signal.result()) {
            case PASS -> {
                // Let vanilla continue.
            }
            case SUCCESS -> cancel(event, InteractionResult.SUCCESS);
            case CONSUME -> cancel(event, InteractionResult.CONSUME);
            case FAIL -> cancel(event, InteractionResult.FAIL);
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }
}
