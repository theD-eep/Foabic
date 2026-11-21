package dev.foabic.api.signal;

import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;

import java.util.Objects;

/**
 * A player right-clicks a block. Listeners may change {@link #result()} to
 * consume the interaction or report success.
 *
 * <p>This is a plain data signal: it deliberately holds no live Minecraft
 * objects. Fabric and NeoForge adapters translate their native interaction
 * result into {@link Result} and back.
 */
public final class UseBlockSignal {

    public enum Hand {
        MAIN_HAND,
        OFF_HAND
    }

    public enum Result {
        PASS,
        SUCCESS,
        CONSUME,
        FAIL
    }

    private final String playerName;
    private final String dimension;
    private final ResourceLocation block;
    private final BlockPos pos;
    private final Hand hand;
    private Result result = Result.PASS;

    public UseBlockSignal(String playerName, String dimension, ResourceLocation block,
                          BlockPos pos, Hand hand) {
        this.playerName = Objects.requireNonNull(playerName, "playerName");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.block = Objects.requireNonNull(block, "block");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.hand = Objects.requireNonNull(hand, "hand");
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

    public Hand hand() {
        return hand;
    }

    public Result result() {
        return result;
    }

    public void setResult(Result result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    @Override
    public String toString() {
        return "UseBlockSignal[" + playerName + " " + block + " at " + pos
                + " hand=" + hand + " result=" + result + "]";
    }
}
