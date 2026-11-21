package dev.foabic.api.internal;

import dev.foabic.api.registry.FoabicCreativeTabs;
import dev.foabic.api.registry.FoabicRegistries;

import java.util.Objects;

/** Internal lifecycle hooks. Adapters call this; mods must not. */
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
        if (runtime == expected) {
            runtime = null;
            FoabicRegistries.unbindAll();
            FoabicCreativeTabs.clear();
        }
    }
}
