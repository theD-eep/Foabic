package dev.foabic.loader;

import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Finds {@code foabic.mod} manifests in {@code mods/*.jar}. There is no
 * classpath scanning: a jar without the manifest is simply not a Foabic mod.
 */
final class ModDiscoverer {

    private ModDiscoverer() {
    }

    static List<DiscoveredMod> discover(Path modsDir, Environment environment, System.Logger log) {
        if (!Files.isDirectory(modsDir)) {
            return List.of();
        }

        List<Path> jars;
        try (var stream = Files.list(modsDir)) {
            jars = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT)
                            .endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new ModLoadException("cannot list mods directory " + modsDir, e);
        }

        List<DiscoveredMod> result = new ArrayList<>();
        Map<String, DiscoveredMod> byId = new HashMap<>();
        for (Path jarPath : jars) {
            DiscoveredMod discovered = read(jarPath, environment, log);
            if (discovered == null) {
                continue;
            }
            DiscoveredMod previous = byId.putIfAbsent(discovered.metadata().id(), discovered);
            if (previous != null) {
                throw new ModLoadException("duplicate mod id '" + discovered.metadata().id()
                        + "' in " + previous.jar().getFileName() + " and " + jarPath.getFileName());
            }
            result.add(discovered);
        }
        return result;
    }

    private static DiscoveredMod read(Path jarPath, Environment environment, System.Logger log) {
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            JarEntry entry = jar.getJarEntry("foabic.mod");
            if (entry == null || entry.isDirectory()) {
                log.log(System.Logger.Level.DEBUG,
                        "ignoring {0}: no foabic.mod", jarPath.getFileName());
                return null;
            }
            if (entry.getSize() > ModMetadataParser.MAX_MANIFEST_BYTES) {
                throw new ModLoadException(jarPath.getFileName() + ": foabic.mod is too large");
            }

            byte[] bytes;
            try (InputStream in = jar.getInputStream(entry)) {
                bytes = in.readNBytes(ModMetadataParser.MAX_MANIFEST_BYTES + 1);
            }
            ModMetadata metadata = ModMetadataParser.parse(
                    bytes, jarPath.getFileName() + "!/foabic.mod");
            if (!metadata.supports(environment)) {
                log.log(System.Logger.Level.INFO,
                        "skipping {0}: declares {1}, running {2}",
                        metadata.id(), metadata.environment(), environment);
                return null;
            }
            return new DiscoveredMod(jarPath, metadata);
        } catch (IOException e) {
            throw new ModLoadException("cannot read mod jar " + jarPath, e);
        }
    }
}
