package dev.foabic.api.signal;

import net.minecraft.server.MinecraftServer;

import java.util.Objects;

/**
 * Posted once by each platform adapter after a server has completed startup.
 * On a client this fires for an integrated server; it does not fire merely
 * because the client connected to a remote server.
 *
 * <p>This is the deterministic acceptance signal used by Foabic 1.0 and 1.1:
 * it does not require a player, a world interaction, or a client, so the same
 * minimal mod can be started twice on Fabric and twice on NeoForge and produce
 * the same observable result.
 */
public final class ServerStartedSignal {

    private final String platform;
    private final String minecraftVersion;
    private final int modCount;
    private final MinecraftServer server;

    public ServerStartedSignal(String platform, String minecraftVersion, int modCount) {
        this(platform, minecraftVersion, modCount, null);
    }

    public ServerStartedSignal(String platform, String minecraftVersion, int modCount,
                               MinecraftServer server) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.modCount = modCount;
        this.server = server;
    }

    public String platform() {
        return platform;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public int modCount() {
        return modCount;
    }

    /**
     * The live server, or {@code null} when the signal is constructed without
     * one (tests and synthetic posts). Adapters always provide it.
     */
    public MinecraftServer server() {
        return server;
    }

    @Override
    public String toString() {
        return "ServerStartedSignal[" + platform + " mc=" + minecraftVersion
                + " mods=" + modCount + "]";
    }
}
