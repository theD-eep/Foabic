package dev.foabic.api;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.internal.FoabicInternal;
import dev.foabic.api.internal.FoabicRuntime;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Public entry point to Foabic.
 *
 * <p>Foabic is a small, platform-neutral mod API. A Foabic mod implements
 * {@link dev.foabic.api.mod.ModInitializer}, registers content through
 * {@link dev.foabic.api.registry.FoabicRegistries}, and subscribes to signals
 * through {@link dev.foabic.api.mod.ModContext#connect(Class, java.util.function.Consumer)}.
 *
 * <p>Version 1.1 targets Minecraft 26.3 and ships two adapters: Fabric and
 * NeoForge. The two platform jars are separate artifacts. There is no
 * universal/merged jar in 1.1.
 */
public final class Foabic {

    /** Foabic API/runtime version. */
    public static final String VERSION = "1.1.0";

    /** The only Minecraft version supported by Foabic 1.1. */
    public static final String MINECRAFT = "26.3";

    /** Version of the public API surface, for mod metadata checks. */
    public static final int API_VERSION = 2;

    private Foabic() {
    }

    /** Returns whether the loader has installed a runtime. */
    public static boolean isReady() {
        return FoabicInternal.isInstalled();
    }

    /**
     * Returns the platform id of the active adapter: {@code "fabric"} or
     * {@code "neoforge"}.
     */
    public static String platform() {
        return runtime().platform();
    }

    /** The shared synchronous signal bus. */
    public static EventBus events() {
        return runtime().events();
    }

    /** The environment the current process is running in. */
    public static Environment environment() {
        return runtime().environment();
    }

    /** The game directory used for {@code foabic-mods/} and {@code .foabic/}. */
    public static Path gameDirectory() {
        return runtime().gameDirectory();
    }

    /** All Foabic mods loaded in this process, in initialization order. */
    public static List<ModContainer> mods() {
        return runtime().mods();
    }

    public static Optional<ModContainer> mod(String id) {
        Objects.requireNonNull(id, "id");
        return runtime().mod(id);
    }

    private static FoabicRuntime runtime() {
        return FoabicInternal.runtime();
    }
}
