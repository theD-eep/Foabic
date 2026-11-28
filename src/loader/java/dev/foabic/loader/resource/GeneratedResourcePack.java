package dev.foabic.loader.resource;

import dev.foabic.api.util.ResourceLocation;
import dev.foabic.loader.mod.ModContextImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Builds a self-contained client resource pack for simple Foabic blocks.
 *
 * <p>The generated pack contains blockstates, cube-all block models, item
 * models and the referenced PNGs. Textures are copied out of Foabic mod jars;
 * a visible fallback texture is used when a declared PNG cannot be found, so a
 * Foabic block never renders as the purple-black missing texture.
 *
 * <p>No model JSON is parsed. A mod that already ships a custom blockstate is
 * left alone unless it explicitly asks Foabic to generate a model.
 */
public final class GeneratedResourcePack {

    public static final String DIRECTORY = "generated-resources";
    public static final String PACK_ID = "foabic:generated";
    public static final int RESOURCE_PACK_FORMAT = 97;
    public static final int RESOURCE_PACK_FORMAT_MINOR = 1;

    private static final String FALLBACK_TEXTURE = "foabic:block/generated_fallback";
    private static final String FALLBACK_TEXTURE_PATH =
            "assets/foabic/textures/block/generated_fallback.png";
    private static final String DEFAULT_PARENT = "minecraft:block/cube_all";
    private static final byte[] FALLBACK_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAe0lEQVR4nGN8VqHxn4ECwMRAIWCBMXbpdTBsWLeGKE0BQSEMbpcqUF2wYd0asAQxmpEtYkKWJGQIumYMA/AZgk0zzkDcgGYILs0ogYjPEHyBy8RAIWACJQFzNqGAZcKnGQbwGcJESDMhQ5iI0YzPELgBhDTjMoRxwHMjALkMRjcdppkgAAAAAElFTkSuQmCC");

    private GeneratedResourcePack() {
    }

    /**
     * Regenerates the pack and returns its root directory. The directory is
     * always created; call {@link #hasAssets(Path)} before registering it as a
     * resource pack.
     */
    public static Path generate(Path stateRoot, List<ModContextImpl> contexts,
                                System.Logger log) {
        Objects.requireNonNull(stateRoot, "stateRoot");
        Objects.requireNonNull(contexts, "contexts");
        Objects.requireNonNull(log, "log");

        Path normalizedState = stateRoot.toAbsolutePath().normalize();
        Path root = normalizedState.resolve(DIRECTORY);
        if (!root.startsWith(normalizedState)) {
            throw new IllegalArgumentException("generated resource path escapes state root");
        }
        deleteRecursively(root);

        Map<String, byte[]> files = new TreeMap<>();
        Set<String> textures = new LinkedHashSet<>();
        List<Path> jars = new ArrayList<>();
        for (ModContextImpl context : contexts) {
            jars.add(context.mod().jar());
        }

        for (ModContextImpl context : contexts) {
            Map<ResourceLocation, BlockModelRequest> explicit = new LinkedHashMap<>();
            for (BlockModelRequest request : context.blockRequests()) {
                explicit.put(request.block(), request);
            }
            Set<ResourceLocation> blocks = new LinkedHashSet<>(context.registeredBlocks());
            blocks.addAll(explicit.keySet());

            boolean fallbackLogged = false;
            for (ResourceLocation block : blocks) {
                BlockModelRequest request = explicit.get(block);
                if (request == null && context.defaultTexture().isBlank()) {
                    if (jarHas(context.mod().jar(), blockstatePath(block))) {
                        continue;
                    }
                    if (!fallbackLogged) {
                        log.log(System.Logger.Level.INFO,
                                "mod {0} declares no texture; generating a fallback model for "
                                        + "its blocks. Set texture in foabic.mod.json to use a real texture.",
                                context.mod().id());
                        fallbackLogged = true;
                    }
                    request = new BlockModelRequest(block, FALLBACK_TEXTURE, DEFAULT_PARENT);
                } else if (request == null) {
                    request = new BlockModelRequest(block, context.defaultTexture(), DEFAULT_PARENT);
                }
                writeBlock(files, textures, jars, context, request, log);
            }

            for (ItemModelRequest request : context.itemRequests()) {
                String json = "{\"parent\":\"" + request.model() + "\"}\n";
                files.put(itemModelPath(request.item()), bytes(json));
            }
        }

        // Runtime data providers can emit assets/ as well as data/. Merge those
        // files first so they override Foabic's fallback model for the block.
        for (ModContextImpl context : contexts) {
            context.dataContext().files().forEach((path, json) -> {
                if (path.startsWith("assets/")) {
                    files.put(path, bytes(json));
                }
            });
        }

        // Foabic mods live in foabic-mods/, so Fabric/NeoForge will not expose
        // their assets/ as a normal built-in resource pack. Copy those files
        // into Foabic's pack, which also makes build-time data-generated models
        // and lang files available at runtime.
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (entry.isDirectory() || !name.startsWith("assets/")
                            || name.equals("pack.mcmeta") || name.contains("..")
                            || name.startsWith("/") || files.containsKey(name)) {
                        continue;
                    }
                    try (InputStream input = jar.getInputStream(entry)) {
                        files.put(name, input.readAllBytes());
                    }
                }
            } catch (IOException e) {
                log.log(System.Logger.Level.WARNING,
                        "cannot scan assets/ entries in {0}: {1}", jarPath, e.getMessage());
            }
        }

        if (textures.contains(FALLBACK_TEXTURE)) {
            files.put(FALLBACK_TEXTURE_PATH, FALLBACK_PNG);
        }

        if (!hasBlockAssets(files)) {
            // Avoid registering an empty pack, but keep the path for adapters.
            try {
                Files.createDirectories(root);
            } catch (IOException e) {
                log.log(System.Logger.Level.ERROR,
                        "cannot create generated resource directory {0}", root);
            }
            return root;
        }

        files.put("pack.mcmeta", bytes("""
                {
                  "pack": {
                    "description": "Foabic generated block assets",
                    "min_format": [%d, %d],
                    "max_format": [%d, %d]
                  }
                }
                """.formatted(RESOURCE_PACK_FORMAT, RESOURCE_PACK_FORMAT_MINOR,
                RESOURCE_PACK_FORMAT, RESOURCE_PACK_FORMAT_MINOR)));

        try {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                Path target = root.resolve(entry.getKey()).normalize();
                if (!target.startsWith(root)) {
                    throw new IOException("generated resource path escapes pack: " + entry.getKey());
                }
                Files.createDirectories(target.getParent());
                Files.write(target, entry.getValue());
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot write generated resource pack " + root, e);
        }

        log.log(System.Logger.Level.INFO,
                "FOABIC_RESOURCES files={0} textures={1} root={2}",
                files.size(), textures.size(), root);
        return root;
    }

    private static void writeBlock(Map<String, byte[]> files, Set<String> textures,
                                   List<Path> jars, ModContextImpl context,
                                   BlockModelRequest request, System.Logger log) {
        ResourceLocation block = request.block();
        String texture = normalizeTexture(request.texture(), block.namespace());
        if (texture == null) {
            log.log(System.Logger.Level.WARNING,
                    "invalid texture id '{0}' for block {1}; using fallback texture",
                    request.texture(), block);
            texture = FALLBACK_TEXTURE;
        }
        String model = block.namespace() + ":block/" + block.path();
        String blockstate = "{\"variants\":{\"\":{\"model\":\"" + model + "\"}}}\n";
        String blockModel = "{\"parent\":\"" + request.modelParent()
                + "\",\"textures\":{\"all\":\"" + texture + "\"}}\n";
        String itemModel = "{\"parent\":\"" + model + "\"}\n";

        files.put(blockstatePath(block), bytes(blockstate));
        files.put(blockModelPath(block), bytes(blockModel));
        files.put(itemModelPath(block), bytes(itemModel));

        textures.add(texture);
        String texturePath = texturePath(texture);
        if (!files.containsKey(texturePath)) {
            byte[] source = findTexture(jars, texturePath);
            if (source == null) {
                if (!texture.equals(FALLBACK_TEXTURE)) {
                    log.log(System.Logger.Level.WARNING,
                            "texture {0} for block {1} was not found in any Foabic mod jar; "
                                    + "using the generated fallback texture. Expected {2}",
                            texture, block, texturePath);
                    texture = FALLBACK_TEXTURE;
                    textures.add(texture);
                    files.put(blockstatePath(block), bytes("{\"variants\":{\"\":{\"model\":\""
                            + model + "\"}}}\n"));
                    files.put(blockModelPath(block), bytes("{\"parent\":\"" + request.modelParent()
                            + "\",\"textures\":{\"all\":\"" + texture + "\"}}\n"));
                    texturePath = texturePath(texture);
                }
                source = FALLBACK_PNG;
            }
            files.put(texturePath, source);
        }
    }

    private static String normalizeTexture(String value, String defaultNamespace) {
        if (value == null) return null;
        String text = value.trim();
        if (text.isEmpty()) return null;
        String namespace = defaultNamespace;
        int colon = text.indexOf(':');
        if (colon >= 0) {
            namespace = text.substring(0, colon);
            text = text.substring(colon + 1);
        }
        if (text.startsWith("assets/")) {
            String prefix = "assets/" + namespace + "/textures/";
            if (text.startsWith(prefix)) {
                text = text.substring(prefix.length());
            } else {
                return null;
            }
        }
        if (text.startsWith("textures/")) text = text.substring("textures/".length());
        if (text.endsWith(".png")) text = text.substring(0, text.length() - 4);
        if (namespace.isBlank() || text.isBlank() || text.startsWith("/") || text.contains("..")) {
            return null;
        }
        try {
            return new ResourceLocation(namespace, text).toString();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] findTexture(List<Path> jars, String texturePath) {
        for (Path jarPath : jars) {
            try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
                byte[] bytes = readEntry(jar, texturePath);
                if (bytes != null) return bytes;
            } catch (IOException ignored) {
                // A broken jar is reported during discovery; keep looking.
            }
        }
        return null;
    }

    private static boolean jarHas(Path jarPath, String entryName) {
        try (JarFile jar = new JarFile(jarPath.toFile(), false)) {
            return jar.getJarEntry(entryName) != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static byte[] readEntry(JarFile jar, String name) throws IOException {
        JarEntry entry = jar.getJarEntry(name);
        if (entry == null || entry.isDirectory()) return null;
        try (InputStream in = jar.getInputStream(entry)) {
            return in.readAllBytes();
        }
    }

    private static String blockstatePath(ResourceLocation block) {
        return "assets/" + block.namespace() + "/blockstates/" + block.path() + ".json";
    }

    private static String blockModelPath(ResourceLocation block) {
        return "assets/" + block.namespace() + "/models/block/" + block.path() + ".json";
    }

    private static String itemModelPath(ResourceLocation item) {
        return "assets/" + item.namespace() + "/models/item/" + item.path() + ".json";
    }

    private static String texturePath(String texture) {
        ResourceLocation id = ResourceLocation.parse(texture);
        return "assets/" + id.namespace() + "/textures/" + id.path() + ".png";
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean hasBlockAssets(Map<String, byte[]> files) {
        return files.keySet().stream().anyMatch(name -> name.startsWith("assets/"));
    }

    /** Whether the pack contains at least one namespace directory. */
    public static boolean hasAssets(Path root) {
        Path assets = root.resolve("assets");
        if (!Files.isDirectory(assets)) return false;
        try (var stream = Files.list(assets)) {
            return stream.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) return;
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new IllegalStateException("cannot clean generated resource pack " + root, e);
        }
    }
}
