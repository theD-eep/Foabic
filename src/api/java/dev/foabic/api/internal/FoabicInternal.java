package dev.foabic.api.internal;

import java.util.Objects;

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
            throw new IllegalStateException("Foabic is not initialized");
        }
        return current;
    }

    public static boolean isInstalled() {
        return runtime != null;
    }

    public static synchronized void uninstall(FoabicRuntime expected) {
        if (runtime == expected) runtime = null;
    }
}
