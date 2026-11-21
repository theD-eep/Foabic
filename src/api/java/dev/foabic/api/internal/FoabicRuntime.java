package dev.foabic.api.internal;

import dev.foabic.api.command.CommandRegistry;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.scheduler.Scheduler;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Internal loader/runtime contract; not part of the mod API. */
public interface FoabicRuntime {
    EventBus events();
    Scheduler scheduler();
    CommandRegistry commands();
    Environment environment();
    Path gameDirectory();
    List<ModContainer> mods();
    Optional<ModContainer> mod(String id);
}
