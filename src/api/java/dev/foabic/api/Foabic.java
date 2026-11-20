package dev.foabic.api;

import dev.foabic.api.command.CommandRegistry;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.internal.FoabicInternal;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.scheduler.Scheduler;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The public entry point to Foabic.
 *
 * <p>All methods except {@link #isReady()} and {@link #VERSION} require a
 * loaded runtime. During a mod's initializer the runtime is guaranteed to be
 * installed.
 */
public final class Foabic {

    /** Foabic API version, not the Minecraft version. */
    public static final String VERSION = "0.1.0";

    /** The Minecraft line this release targets. */
    public static final String MINECRAFT = "26.n+";

    private Foabic() {
    }

    public static boolean isReady() {
        return FoabicInternal.isInstalled();
    }

    public static EventBus events() {
        return runtime().events();
    }

    public static Scheduler scheduler() {
        return runtime().scheduler();
    }

    public static CommandRegistry commands() {
        return runtime().commands();
    }

    public static Environment environment() {
        return runtime().environment();
    }

    public static Path gameDirectory() {
        return runtime().gameDirectory();
    }

    public static List<ModContainer> mods() {
        return runtime().mods();
    }

    public static Optional<ModContainer> mod(String id) {
        Objects.requireNonNull(id, "id");
        return runtime().mod(id);
    }

    private static dev.foabic.api.internal.FoabicRuntime runtime() {
        return FoabicInternal.runtime();
    }
}
