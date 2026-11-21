package dev.foabic.api.signal;

import dev.foabic.api.game.Level;
import dev.foabic.api.game.Player;
import dev.foabic.api.util.BlockPos;
import dev.foabic.api.util.ResourceLocation;

import java.util.Objects;

/**
 * A player right-clicks a block. Listeners may change {@link #result()} to
 * consume the interaction or report success.
 *
 * <p>This is intentionally one concrete signal, not a general signal standard
 * library. Foabic 0.2 only ships the hooks needed by the Fabric adapter.
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

    private final Player player;
    private final Level level;
    private final ResourceLocation block;
    private final BlockPos pos;
    private final Hand hand;
    private Result result = Result.PASS;

    public UseBlockSignal(Player player, Level level, ResourceLocation block,
                          BlockPos pos, Hand hand) {
        this.player = Objects.requireNonNull(player, "player");
        this.level = Objects.requireNonNull(level, "level");
        this.block = Objects.requireNonNull(block, "block");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.hand = Objects.requireNonNull(hand, "hand");
    }

    public Player player() {
        return player;
    }

    public Level level() {
        return level;
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
        return "UseBlockSignal[" + player.name() + " " + block + " at " + pos + "]";
    }
}
