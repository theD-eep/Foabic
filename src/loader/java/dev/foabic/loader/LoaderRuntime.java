package dev.foabic.loader;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.internal.FoabicRuntime;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class LoaderRuntime implements FoabicRuntime {

    private final EventBus events = new EventBus();
    private final String platform;
    private final Environment environment;
    private final Path gameDirectory;
    private final List<ModContainer> mods;
    private final Map<String, ModContainer> byId = new LinkedHashMap<>();

    LoaderRuntime(String platform, Environment environment, Path gameDirectory,
                  List<ModContainer> mods) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory");
        this.mods = List.copyOf(mods);
        for (ModContainer mod : this.mods) byId.put(mod.id(), mod);
    }

    @Override public String platform() { return platform; }
    @Override public EventBus events() { return events; }
    @Override public Environment environment() { return environment; }
    @Override public Path gameDirectory() { return gameDirectory; }
    @Override public List<ModContainer> mods() { return mods; }

    @Override
    public Optional<ModContainer> mod(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
