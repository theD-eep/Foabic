package dev.foabic.mc26.spi;

/**
 * The narrow, Minecraft-free contract between the system-loader hook pad and
 * the bridge loaded next to Minecraft's classes.
 *
 * <p>A vanilla {@code server.jar} launches Minecraft in a child URL class
 * loader. The injected hook class is visible to that loader through its parent,
 * but a system-loaded bridge could not link against Minecraft classes. The
 * bridge implementation is therefore loaded in a child-first adapter loader
 * whose parent is the Minecraft class loader.
 */
public interface HookBridge {

    void serverStarting(Object raw);

    void serverTickStart(Object raw);

    void serverTickEnd(Object raw);

    void serverStopping(Object raw);

    void serverStopped(Object raw);

    void levelTickStart(Object raw);

    void levelTickEnd(Object raw);

    void playerJoin(Object raw);

    void playerLeave(Object raw);
}
