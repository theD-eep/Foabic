package dev.foabic.loader;

import dev.foabic.api.command.CommandRegistry;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.internal.FoabicRuntime;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.scheduler.Scheduler;
import dev.foabic.loader.command.CommandRegistryImpl;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LoaderRuntime implements FoabicRuntime {

    private final EventBus events;
    private final Scheduler scheduler = new Scheduler();
    private final CommandRegistryImpl commands = new CommandRegistryImpl();
    private final Environment environment;
    private final Path gameDirectory;
    private final List<ModContainer> mods;
    private final Map<String, ModContainer> byId;

    LoaderRuntime(Environment environment, Path gameDirectory, List<ModContainer> mods) {
        this.environment = Objects.requireNonNull(environment, "environment");
        this.gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory");
        this.mods = List.copyOf(mods);
        this.events = new EventBus();
        this.byId = new LinkedHashMap<>();
        for (ModContainer mod : this.mods) {
            byId.put(mod.id(), mod);
        }
    }

    @Override
    public EventBus events() {
        return events;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public CommandRegistry commands() {
        return commands;
    }

    @Override
    public Environment environment() {
        return environment;
    }

    @Override
    public Path gameDirectory() {
        return gameDirectory;
    }

    @Override
    public List<ModContainer> mods() {
        return mods;
    }

    @Override
    public Optional<ModContainer> mod(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
