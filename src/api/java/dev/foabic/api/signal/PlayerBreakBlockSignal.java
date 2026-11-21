package dev.foabic.api.signal;

import dev.foabic.api.event.CancellableEvent;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;

import java.util.Objects;

/**
 * A player is about to break a block on the logical server.
 *
 * <p>The signal is cancellable: when a listener calls {@link #cancel()}, both
 * adapters abort the block break. Observers can leave it untouched.
 */
public final class PlayerBreakBlockSignal extends CancellableEvent {

    private final String playerName;
    private final String dimension;
    private final ResourceLocation block;
    private final BlockPos pos;

    public PlayerBreakBlockSignal(String playerName, String dimension, ResourceLocation block,
                                  BlockPos pos) {
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.block = Objects.requireNonNull(block, "block");
        this.pos = Objects.requireNonNull(pos, "pos");
    }

    public String playerName() {
        return playerName;
    }

    public String dimension() {
        return dimension;
    }

    public ResourceLocation block() {
        return block;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public String toString() {
        return "PlayerBreakBlockSignal[" + playerName + " " + block + " at " + pos
                + " cancelled=" + isCancelled() + "]";
    }
}
