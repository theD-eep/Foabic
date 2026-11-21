package dev.foabic.api.signal;

import java.util.Objects;

/**
 * Posted once at the end of every server tick, after the level and entity
 * ticks. This is the standard hook for lightweight scheduled work.
 *
 * <p>There is no cancellation and no live server object. Listeners that need
 * to do work on the server thread can do it directly in the callback; heavy
 * work should be scheduled or throttled by the listener.
 *
 * @param platform    {@code "fabric"} or {@code "neoforge"}
 * @param tick        the server tick counter
 * @param playerCount number of players currently online
 */
public record ServerTickSignal(String platform, long tick, int playerCount) {

    public ServerTickSignal {
        Objects.requireNonNull(platform, "platform");
        if (tick < 0) throw new IllegalArgumentException("tick must be >= 0");
        if (playerCount < 0) throw new IllegalArgumentException("playerCount must be >= 0");
    }

    @Override
    public String toString() {
        return "ServerTickSignal[" + platform + " tick=" + tick
                + " players=" + playerCount + "]";
    }
}
