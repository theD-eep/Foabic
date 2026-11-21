package dev.foabic.api.network;

/**
 * A platform-neutral network message.
 *
 * <p>The same packet class works on Fabric and NeoForge. The adapter wraps the
 * byte array in its native payload type and dispatches {@link #handle()} on the
 * game thread. Packets are limited to 1 MiB per message.
 */
public interface FoabicPacket {

    /** Serializes this packet. The returned array must not be null. */
    byte[] encode();

    /** Replaces this packet's state from the supplied bytes. */
    void decode(byte[] data);

    /** Called on the game thread after a packet has been received. */
    default void handle() {
    }
}
