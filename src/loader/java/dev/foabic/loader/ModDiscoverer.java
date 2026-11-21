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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Tolerant mod-jar discovery.
 *
 * <p>A broken jar or malformed {@code foabic.mod.json} is reported and skipped; it
 * must not take down the game. Duplicate ids are also skipped, with an ERROR
 * line naming the id. Dependency failures are different: they are detected
 * before any entrypoint class is loaded and reported as a clear
 * {@link DependencyException} by {@link DependencyResolver}.
 */
final class ModDiscoverer {

    private ModDiscoverer() {
    }

    static List<DiscoveredMod> discover(Path modsDir, Environment environment, System.Logger log) {
        return discover(List.of(modsDir), environment, log);
    }

    /**
     * Scans one or more mod directories. The adapter normally passes
     * {@code foabic-mods/}; multiple directories are supported so a host can
     * provide an additional platform-specific location without changing the
     * loader.
     */
    static List<DiscoveredMod> discover(List<Path> modsDirs, Environment environment, System.Logger log) {
        List<Path> jars = new ArrayList<>();
        for (Path modsDir : modsDirs) {
            if (!Files.isDirectory(modsDir)) continue;
            try (var stream = Files.list(modsDir)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                        .forEach(jars::add);
            } catch (IOException | RuntimeException e) {
                log.log(System.Logger.Level.ERROR,
                        "无法读取模组目录 {0}，已跳过: {1}", modsDir, e.getMessage());
            }
        }

        List<DiscoveredMod> result = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        Map<String, Path> loadedFrom = new HashMap<>();
        for (Path jarPath : jars) {
            DiscoveredMod discovered;
            try {
                discovered = read(jarPath, environment, log);
            } catch (RuntimeException e) {
                log.log(System.Logger.Level.ERROR,
                        "无法加载模组 {0}，已跳过: {1}", jarPath.getFileName(), e.getMessage());
                continue;
            }
            if (discovered == null) continue;

            String id = discovered.metadata().id();
            if (!seenIds.add(id)) {
                log.log(System.Logger.Level.ERROR,
                        "发现重复的 Mod ID: {0}，已跳过加载 ({1}); 已加载自 {2}",
                        id, jarPath, loadedFrom.get(id));
                continue;
            }
            loadedFrom.put(id, jarPath);
            result.add(discovered);
        }
        return result;
    }

    private static DiscoveredMod read(Path jarPath, Environment environment, System.Logger log) {
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            JarEntry entry = jar.getJarEntry("foabic.mod.json");
            String entryName = "foabic.mod.json";
            if (entry == null || entry.isDirectory()) {
                entry = jar.getJarEntry("foabic.mod");
                entryName = "foabic.mod";
            }
            if (entry == null || entry.isDirectory()) {
                log.log(System.Logger.Level.DEBUG, "ignoring {0}: no foabic.mod.json", jarPath.getFileName());
                return null;
            }
            if (entry.getSize() > ModMetadataParser.MAX_BYTES) {
                throw new ModLoadException(jarPath.getFileName() + ": " + entryName + " is too large");
            }
            byte[] bytes;
            try (InputStream in = jar.getInputStream(entry)) {
                bytes = in.readNBytes(ModMetadataParser.MAX_BYTES + 1);
            }
            ModMetadata metadata = ModMetadataParser.parse(bytes,
                    jarPath.getFileName() + "!/" + entryName);
            if (!metadata.supports(environment)) {
                log.log(System.Logger.Level.INFO, "skipping {0}: declares {1}, running {2}",
                        metadata.id(), metadata.environment(), environment);
                return null;
            }
            return new DiscoveredMod(jarPath, metadata);
        } catch (IOException e) {
            throw new ModLoadException("cannot read mod jar " + jarPath, e);
        }
    }
}
