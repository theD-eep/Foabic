package dev.foabic.api.network;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Platform-neutral packet registration and delivery.
 *
 * <p>Register each packet class once during mod initialization:
 * <pre>{@code
 * NetworkManager.register(MyPacket.class, MyPacket::new);
 * }</pre>
 *
 * <p>The transport is installed by the active Fabric/NeoForge adapter before
 * mods are initialized. {@code player} in {@link #sendToClient(Object,
 * FoabicPacket)} must be the adapter's server-player object; the common API
 * does not expose it so that the core jar stays free of Minecraft classes.
 */
public final class NetworkManager {

    private static final System.Logger LOG = System.getLogger("Foabic/network");
    private static final int MAX_PACKET_BYTES = 1024 * 1024;
    private static final Map<String, Supplier<? extends FoabicPacket>> PACKETS =
            new ConcurrentHashMap<>();
    private static volatile Transport transport;

    private NetworkManager() {
    }

    /** Registers a packet type. Calling this twice with the same factory is harmless. */
    public static <P extends FoabicPacket> void register(Class<P> type,
                                                         Supplier<P> factory) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(factory, "factory");
        Supplier<? extends FoabicPacket> previous =
                PACKETS.putIfAbsent(type.getName(), factory);
        if (previous != null && previous != factory) {
            throw new IllegalStateException("packet already registered: " + type.getName());
        }
    }

    public static void sendToServer(FoabicPacket packet) {
        Objects.requireNonNull(packet, "packet");
        transport().sendToServer(packet.getClass().getName(), encode(packet));
    }

    /**
     * Sends to one player. {@code player} must be a {@code ServerPlayer} on the
     * active platform.
     */
    public static void sendToClient(Object player, FoabicPacket packet) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(packet, "packet");
        transport().sendToClient(player, packet.getClass().getName(), encode(packet));
    }

    /**
     * Decodes and handles one received packet. Adapters call this on the game
     * thread; unknown or malformed packets are logged and dropped.
     */
    public static void receive(String type, byte[] data) {
        if (type == null || data == null || data.length > MAX_PACKET_BYTES) {
            LOG.log(System.Logger.Level.WARNING, "dropped malformed Foabic packet");
            return;
        }
        Supplier<? extends FoabicPacket> factory = PACKETS.get(type);
        if (factory == null) {
            LOG.log(System.Logger.Level.WARNING, "dropped unknown Foabic packet {0}", type);
            return;
        }
        try {
            FoabicPacket packet = Objects.requireNonNull(factory.get(),
                    "packet factory returned null for " + type);
            packet.decode(Arrays.copyOf(data, data.length));
            packet.handle();
        } catch (Throwable failure) {
            LOG.log(System.Logger.Level.ERROR, "Foabic packet handler failed for " + type, failure);
        }
    }

    /** Installed by the adapter. */
    public static void bind(Transport newTransport) {
        Objects.requireNonNull(newTransport, "newTransport");
        Transport current = transport;
        if (current != null && current != newTransport) {
            throw new IllegalStateException("Foabic network transport is already bound");
        }
        transport = newTransport;
    }

    /** Removes packet registrations and the transport. Used by tests/reload. */
    public static void reset() {
        PACKETS.clear();
        transport = null;
    }

    private static byte[] encode(FoabicPacket packet) {
        byte[] data = Objects.requireNonNull(packet.encode(), "packet.encode() returned null");
        if (data.length > MAX_PACKET_BYTES) {
            throw new IllegalArgumentException("Foabic packet exceeds " + MAX_PACKET_BYTES + " bytes");
        }
        return Arrays.copyOf(data, data.length);
    }

    private static Transport transport() {
        Transport current = transport;
        if (current == null) {
            throw new IllegalStateException("Foabic networking is not available; "
                    + "is a platform adapter installed?");
        }
        return current;
    }

    /** Platform seam implemented by each adapter. */
    public interface Transport {
        void sendToServer(String type, byte[] data);

        void sendToClient(Object player, String type, byte[] data);
    }
}
