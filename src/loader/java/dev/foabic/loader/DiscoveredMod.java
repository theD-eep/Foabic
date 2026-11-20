package dev.foabic.loader;

import dev.foabic.api.mod.ModMetadata;

import java.nio.file.Path;

record DiscoveredMod(Path jar, ModMetadata metadata) {
}
