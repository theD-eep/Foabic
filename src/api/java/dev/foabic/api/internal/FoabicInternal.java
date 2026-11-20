package dev.foabic.api.internal;

import java.util.Objects;

/**
 * Internal installation point. Not part of the public modding API.
 */
public final class FoabicInternal {

    private static volatile FoabicRuntime runtime;

    private FoabicInternal() {
    }

    public static synchronized void install(FoabicRuntime newRuntime) {
        Objects.requireNonNull(newRuntime, "newRuntime");
        if (runtime != null && runtime != newRuntime) {
            throw new IllegalStateException("Foabic is already initialized");
        }
        runtime = newRuntime;
    }

    public static FoabicRuntime runtime() {
        FoabicRuntime current = runtime;
        if (current == null) {
            throw new IllegalStateException(
                    "Foabic is not initialized. Launch the game with the Foabic java agent "
                            + "(-javaagent:foabic.jar) or call FoabicLoader.load(...) first.");
        }
        return current;
    }

    public static boolean isInstalled() {
        return runtime != null;
    }

    public static synchronized void uninstall(FoabicRuntime expected) {
        if (runtime == expected) {
            runtime = null;
        }
    }
}
