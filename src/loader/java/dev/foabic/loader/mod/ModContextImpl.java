package dev.foabic.loader.mod;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.internal.FoabicRuntime;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.mod.ModContext;

import java.util.Objects;

public final class ModContextImpl implements ModContext {

    private final FoabicRuntime runtime;
    private final ModContainerImpl container;

    public ModContextImpl(FoabicRuntime runtime, ModContainerImpl container) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.container = Objects.requireNonNull(container, "container");
    }

    @Override public ModContainer mod() { return container; }
    @Override public EventBus events() { return runtime.events(); }
    @Override public Environment environment() { return runtime.environment(); }
    @Override public String platform() { return runtime.platform(); }
    @Override public System.Logger logger() { return container.logger(); }
}
