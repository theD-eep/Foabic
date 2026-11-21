package dev.foabic.api.internal;

import dev.foabic.api.event.EventBus;
import dev.foabic.api.mod.ModContainer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Internal loader/runtime contract. Not part of the supported mod API. */
public interface FoabicRuntime {

    String platform();

    EventBus events();

    Path gameDirectory();

    List<ModContainer> mods();

    Optional<ModContainer> mod(String id);
}
