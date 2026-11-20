package dev.foabic.mc26;

import dev.foabic.mc26.spi.HookBridge;

/**
 * Static landing pad for injected bytecode.
 *
 * <p>Every signature deliberately uses {@link Object}: this class is loaded by
 * the application class loader and must be resolvable without Minecraft classes.
 * On first use it creates a child-first bridge next to Minecraft's own class
 * loader, which is what allows both a flat classpath launch and the vanilla
 * {@code server.jar} bundler launch to work.
 */
public final class Mc26Hooks {

    private static final System.Logger LOG = System.getLogger("Foabic/mc26");

    private static volatile HookBridge bridge;
    private static volatile boolean bridgeFailed;

    private Mc26Hooks() {
    }

    public static void serverStarting(Object server) {
        HookBridge target = bridge(server);
        if (target != null) {
            try {
                target.serverStarting(server);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "serverStarting hook failed", failure);
            }
        }
    }

    public static void serverTickStart(Object server) {
        HookBridge target = bridge(server);
        if (target != null) {
            try {
                target.serverTickStart(server);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "serverTickStart hook failed", failure);
            }
        }
    }

    public static void serverTickEnd(Object server) {
        HookBridge target = bridge(server);
        if (target != null) {
            try {
                target.serverTickEnd(server);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "serverTickEnd hook failed", failure);
            }
        }
    }

    public static void serverStopping(Object server) {
        HookBridge target = bridge(server);
        if (target != null) {
            try {
                target.serverStopping(server);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "serverStopping hook failed", failure);
            }
        }
    }

    public static void serverStopped(Object server) {
        HookBridge target = bridge(server);
        if (target != null) {
            try {
                target.serverStopped(server);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "serverStopped hook failed", failure);
            }
        }
    }

    public static void levelTickStart(Object level) {
        HookBridge target = bridge(level);
        if (target != null) {
            try {
                target.levelTickStart(level);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "levelTickStart hook failed", failure);
            }
        }
    }

    public static void levelTickEnd(Object level) {
        HookBridge target = bridge(level);
        if (target != null) {
            try {
                target.levelTickEnd(level);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "levelTickEnd hook failed", failure);
            }
        }
    }

    public static void playerJoin(Object player) {
        HookBridge target = bridge(player);
        if (target != null) {
            try {
                target.playerJoin(player);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "playerJoin hook failed", failure);
            }
        }
    }

    public static void playerLeave(Object player) {
        HookBridge target = bridge(player);
        if (target != null) {
            try {
                target.playerLeave(player);
            } catch (Throwable failure) {
                LOG.log(System.Logger.Level.ERROR, "playerLeave hook failed", failure);
            }
        }
    }

    private static HookBridge bridge(Object raw) {
        HookBridge current = bridge;
        if (current != null) {
            return current;
        }
        if (bridgeFailed) {
            return null;
        }
        synchronized (Mc26Hooks.class) {
            current = bridge;
            if (current != null) {
                return current;
            }
            try {
                ClassLoader minecraftLoader = raw == null ? null : raw.getClass().getClassLoader();
                current = AdapterLoader.create(minecraftLoader);
                bridge = current;
                return current;
            } catch (Throwable failure) {
                bridgeFailed = true;
                LOG.log(System.Logger.Level.ERROR,
                        "cannot initialize Minecraft 26.x bridge", failure);
                return null;
            }
        }
    }
}
