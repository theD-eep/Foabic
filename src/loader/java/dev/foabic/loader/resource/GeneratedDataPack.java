package dev.foabic.loader.resource;

import dev.foabic.loader.mod.ModContextImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Builds a server data pack from Foabic data providers and from {@code data/}
 * entries shipped inside Foabic mod jars.
 *
 * <p>Fabric/NeoForge do not see a jar in {@code foabic-mods/} as a platform
 * mod, so its standard {@code data/} directory would otherwise be ignored.
 * Foabic lifts those files into one always-on data pack.
 */
public final class GeneratedDataPack {

    public static final String DIRECTORY = "generated-data";
    /** Minecraft 26.3 data-pack format. */
    public static final int DATA_PACK_FORMAT = 121;

    private GeneratedDataPack() {
    }

    public static Path generate(Path stateRoot, List<ModContextImpl> contexts,
                                System.Logger log) {
        Objects.requireNonNull(stateRoot, "stateRoot");
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(log, "log");

        Path normalizedState = stateRoot.toAbsolutePath().normalize();
        Path root = normalizedState.resolve(DIRECTORY);
        if (!root.startsWith(normalizedState)) {
            throw new IllegalArgumentException("generated data path escapes state root");
        }
        deleteRecursively(root);

        Map<String, byte[]> files = new TreeMap<>();
        List<Path> jars = new ArrayList<>();
        long providerFiles = 0;
        for (ModContextImpl context : contexts) {
            providerFiles += context.dataContext().dataFileCount();
            context.dataContext().files().forEach((path, json) ->
                    files.put(path, json.getBytes(StandardCharsets.UTF_8)));
            jars.add(context.mod().jar());
        }

        int jarFiles = 0;
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.startsWith("data/")
                            || name.equals("pack.mcmeta")) {
                        continue;
                    }
                    if (name.contains("..") || name.startsWith("/")) continue;
                    if (files.containsKey(name)) continue;
                    try (InputStream input = jar.getInputStream(entry)) {
                        files.put(name, input.readAllBytes());
                        jarFiles++;
                    }
                }
            } catch (IOException e) {
                log.log(System.Logger.Level.WARNING,
                        "cannot scan data/ entries in {0}: {1}", jarPath, e.getMessage());
            }
        }

        if (files.isEmpty()) {
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                log.log(System.Logger.Level.ERROR,
                        "cannot create generated data directory {0}", root);
            }
            return root;
        }

        files.put("pack.mcmeta", bytes("""
                {
                  "pack": {
                    "description": "Foabic generated recipes, tags and loot",
                    "min_format": [%d, 0],
                    "max_format": [%d, 0]
                  }
                }
                """.formatted(DATA_PACK_FORMAT, DATA_PACK_FORMAT)));

        try {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                Path target = root.resolve(entry.getKey()).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException("generated data path escapes pack: " + entry.getKey());
                }
                Files.createDirectories(target.getParent());
                Files.write(target, entry.getValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot write generated data pack " + root, e);
        }

        long recipes = files.keySet().stream()
                .filter(name -> name.startsWith("data/") && name.contains("/recipe/")).count();
        long tags = files.keySet().stream()
                .filter(name -> name.startsWith("data/") && name.contains("/tags/")).count();
        long loot = files.keySet().stream()
                .filter(name -> name.startsWith("data/") && name.contains("/loot_table/")).count();
        log.log(System.Logger.Level.INFO,
                "FOABIC_DATA files={0} provider={1} jar={2} recipes={3} tags={4} loot={5} root={6}",
                files.size() - 1, providerFiles, jarFiles, recipes, tags, loot, root);
        return root;
    }

    public static boolean hasData(Path root) {
        Path data = root.resolve("data");
        if (!Files.isDirectory(data)) return false;
        try (var stream = Files.list(data)) {
            return stream.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot clean generated data pack " + root, e);
        }
    }
}
