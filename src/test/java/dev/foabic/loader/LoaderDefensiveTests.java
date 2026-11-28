package dev.foabic.loader;

import dev.foabic.api.mod.Dependency;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModMetadata;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.registry.FoabicRegistryFrozenException;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.loader.config.ConfigManagerImpl;
import dev.foabic.loader.mod.ModContainerImpl;
import dev.foabic.loader.mod.ModContextImpl;
import dev.foabic.loader.resource.GeneratedResourcePack;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Regression tests for the defensive loader, metadata formats, registry freeze
 * protection, configuration and generated-resource path.
 */
public final class LoaderDefensiveTests {

    private LoaderDefensiveTests() {
    }

    public static void run() {
        testDuplicateIdIsSkipped();
        testMalformedModIsSkipped();
        testBomMetadataIsTolerated();
        testJsonMetadata();
        testMissingPlatformDependencyIsClear();
        testProvidedPlatformDependencyResolves();
        testRegistryFreezeIsCaught();
        testConfigRoundTrip();
        testGeneratedBlockAssets();
        testMissingTextureFallsBack();
    }

    private static void testDuplicateIdIsSkipped() {
        Path root = tempDir();
        Path mods = createDirectory(root.resolve("mods"));
        writeJar(mods.resolve("one.jar"), metadataText("duplicate_test", "^1.2.0", ""));
        writeJar(mods.resolve("two.jar"), metadataText("duplicate_test", "^1.2.0", ""));

        RecordingLogger logger = new RecordingLogger();
        List<DiscoveredMod> modsFound = ModDiscoverer.discover(mods, Environment.SERVER, logger);
        check(modsFound.size() == 1, "duplicate discovery count " + modsFound.size());
        check(logger.errors.stream().anyMatch(line ->
                        line.contains("发现重复的 Mod ID: duplicate_test")),
                "duplicate ERROR line not found: " + logger.errors);
    }

    private static void testMalformedModIsSkipped() {
        Path root = tempDir();
        Path mods = createDirectory(root.resolve("mods"));
        writeJar(mods.resolve("bad.jar"), "id=Bad_Id\nversion=1.0.0\nentrypoint=test.Bad\n");

        RecordingLogger logger = new RecordingLogger();
        List<DiscoveredMod> modsFound = ModDiscoverer.discover(mods, Environment.SERVER, logger);
        check(modsFound.isEmpty(), "malformed mod was not skipped");
        check(logger.errors.stream().anyMatch(line -> line.contains("无法加载模组")),
                "malformed mod ERROR line not found: " + logger.errors);
    }

    private static void testBomMetadataIsTolerated() {
        Path root = tempDir();
        Path mods = createDirectory(root.resolve("mods"));
        writeJar(mods.resolve("bom.jar"), "\uFEFF" + metadataText("bom_test", "^1.2.0", ""));

        RecordingLogger logger = new RecordingLogger();
        List<DiscoveredMod> modsFound = ModDiscoverer.discover(mods, Environment.SERVER, logger);
        check(modsFound.size() == 1, "BOM metadata was not accepted: " + logger.errors);
        check(modsFound.getFirst().metadata().id().equals("bom_test"), "BOM metadata id");
    }

    private static void testJsonMetadata() {
        Path root = tempDir();
        Path mods = createDirectory(root.resolve("mods"));
        String json = """
                {
                  "schemaVersion": 1,
                  "id": "json_test",
                  "name": "JSON Test",
                  "version": "1.3.0",
                  "entrypoint": "test.Noop",
                  "environment": "*",
                  "api": "^1.3.0",
                  "data": "test.Data",
                  "texture": "json_test:block/test",
                  "icon": "icon.png",
                  "depends": {"foabic": ">=1.3.0"}
                }
                """;
        writeJar(mods.resolve("json.jar"), json);

        RecordingLogger logger = new RecordingLogger();
        List<DiscoveredMod> modsFound = ModDiscoverer.discover(mods, Environment.SERVER, logger);
        check(modsFound.size() == 1, "JSON metadata was not accepted: " + logger.errors);
        ModMetadata metadata = modsFound.getFirst().metadata();
        check(metadata.version().equals("1.3.0"), "JSON version");
        check(metadata.entrypoints().equals(List.of("test.Noop")), "JSON entrypoint");
        check(metadata.dependencies().size() == 1, "JSON dependencies");
        check(metadata.dependencies().getFirst().range().equals(">=1.3.0"), "JSON range");
        check(metadata.texture().equals("json_test:block/test"), "JSON texture");
        check(metadata.icon().equals("icon.png"), "JSON icon");
        check(metadata.dataProviders().equals(List.of("test.Data")), "JSON data provider");
    }

    private static void testMissingPlatformDependencyIsClear() {
        Dependency fabricApi = new Dependency("fabric-api", ">=0.166.2");
        DiscoveredMod mod = mod("needs_fabric_api", "^1.2.0", List.of(fabricApi));
        try {
            new DependencyResolver(List.of(mod), Map.of(), "neoforge",
                    new RecordingLogger()).resolve();
            throw new AssertionError("missing fabric-api dependency was accepted");
        } catch (DependencyException e) {
            check(e.getMessage().contains("Fabric API"), "dependency message: " + e.getMessage());
            check(e.getMessage().contains("Fabric-only"), "dependency hint: " + e.getMessage());
        }
    }

    private static void testProvidedPlatformDependencyResolves() {
        Dependency fabricApi = new Dependency("fabric-api", ">=0.166.2");
        DiscoveredMod mod = mod("needs_fabric_api", "^1.2.0", List.of(fabricApi));
        List<DiscoveredMod> ordered = new DependencyResolver(List.of(mod),
                Map.of("fabric-api", "0.166.2+26.3"), "fabric", new RecordingLogger()).resolve();
        check(ordered.size() == 1, "provided dependency resolution failed");
    }

    private static void testRegistryFreezeIsCaught() {
        RecordingLogger logger = new RecordingLogger();
        FoabicRegistries.freezeRegistration();
        try {
            FoabicRegistries.checkOpen("freeze_test",
                    ResourceLocation.of("freeze_test", "thing"), logger);
            throw new AssertionError("frozen registry accepted a submission");
        } catch (FoabicRegistryFrozenException e) {
            String expected = "[Foabic] 模组 freeze_test 试图在注册表冻结后注册物品 "
                    + "freeze_test:thing，已被拦截。";
            check(e.getMessage().equals(expected), "freeze exception message: " + e.getMessage());
            check(logger.errors.contains(expected), "freeze ERROR line: " + logger.errors);
            check(e.modId().equals("freeze_test"), "freeze mod id");
        } finally {
            FoabicRegistries.unbindAll();
        }
    }

    private static void testConfigRoundTrip() {
        Path root = tempDir();
        RecordingLogger logger = new RecordingLogger();
        ConfigManagerImpl manager = new ConfigManagerImpl(root, "config_test", logger);
        SampleConfig config = manager.register(SampleConfig.class);
        check(config.name.equals("default"), "config default name: " + config.name);
        check(config.count == 3, "config default count: " + config.count);
        config.name = "changed";
        config.count = 9;
        manager.save(config);

        ConfigManagerImpl reloaded = new ConfigManagerImpl(root, "config_test", logger);
        SampleConfig loaded = reloaded.register(SampleConfig.class);
        check(loaded.name.equals("changed"), "config reload name: " + loaded.name);
        check(loaded.count == 9, "config reload count: " + loaded.count);

        RecordConfig record = new RecordConfig("record-value", 12);
        reloaded.save(record);
        ConfigManagerImpl recordManager = new ConfigManagerImpl(root, "config_test", logger);
        RecordConfig loadedRecord = recordManager.register(RecordConfig.class);
        check(loadedRecord.name().equals("record-value"), "record config name");
        check(loadedRecord.count() == 12, "record config count");
    }

    private static void testGeneratedBlockAssets() {
        Path root = tempDir();
        byte[] png = "fake-png".getBytes(StandardCharsets.UTF_8);
        Path jar = writeJar(root.resolve("demo.jar"), metadataText("demo", "^1.2.0", ""),
                Map.of("assets/demo/textures/block/test.png", png));

        ModContextImpl context = context(root, jar, "demo", "");
        context.block(ResourceLocation.of("demo", "test_block"),
                "demo:block/test", "minecraft:block/cube_all");
        Path pack = GeneratedResourcePack.generate(root.resolve(".foabic"), List.of(context),
                new RecordingLogger());

        Path blockstate = pack.resolve("assets/demo/blockstates/test_block.json");
        Path model = pack.resolve("assets/demo/models/block/test_block.json");
        Path texture = pack.resolve("assets/demo/textures/block/test.png");
        check(Files.isRegularFile(blockstate), "missing blockstate " + blockstate);
        check(Files.isRegularFile(model), "missing block model " + model);
        check(Files.isRegularFile(texture), "missing copied texture " + texture);
        try {
            check(java.util.Arrays.equals(Files.readAllBytes(texture), png), "texture copy differs");
            check(Files.readString(model).contains("\"all\":\"demo:block/test\""),
                    "model texture id: " + Files.readString(model));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        check(GeneratedResourcePack.hasAssets(pack), "pack has no assets");
    }

    private static void testMissingTextureFallsBack() {
        Path root = tempDir();
        Path jar = writeJar(root.resolve("demo.jar"), metadataText("demo", "^1.2.0", ""),
                Map.of());
        ModContextImpl context = context(root, jar, "demo", "");
        context.block(ResourceLocation.of("demo", "fallback_block"),
                "demo:block/does_not_exist", "minecraft:block/cube_all");
        Path pack = GeneratedResourcePack.generate(root.resolve(".foabic"), List.of(context),
                new RecordingLogger());
        Path fallback = pack.resolve("assets/foabic/textures/block/generated_fallback.png");
        check(Files.isRegularFile(fallback), "missing fallback texture");
        try {
            check(Files.readString(pack.resolve("assets/demo/models/block/fallback_block.json"))
                            .contains("foabic:block/generated_fallback"),
                    "fallback texture was not used in the model");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static ModContextImpl context(Path root, Path jar, String id, String texture) {
        ModMetadata metadata = metadata(id, "^1.2.0", List.of(), texture);
        ModContainerImpl container = new ModContainerImpl(metadata, jar,
                createDirectory(root.resolve("data")), LoaderDefensiveTests.class.getClassLoader());
        LoaderRuntime runtime = new LoaderRuntime("fabric", root, List.of(container));
        return new ModContextImpl(runtime, container);
    }

    private static DiscoveredMod mod(String id, String api, List<Dependency> dependencies) {
        Path jar = tempDir().resolve(id + ".jar");
        ModMetadata metadata = new ModMetadata(id, id, "1.0.0", "",
                List.of(), List.of("test.Noop"), dependencies, List.of(),
                Environment.SERVER, api, "", "", List.of());
        return new DiscoveredMod(jar, metadata);
    }

    private static ModMetadata metadata(String id, String api, List<Dependency> dependencies,
                                        String texture) {
        return new ModMetadata(id, id, "1.2.0", "",
                List.of(), List.of("test.Noop"), dependencies, List.of(),
                Environment.SERVER, api, texture, "", List.of());
    }

    private static String metadataText(String id, String api, String texture) {
        return "id=" + id + "\n"
                + "name=" + id + "\n"
                + "version=1.2.0\n"
                + "entrypoint=test.Noop\n"
                + "environment=*\n"
                + "api=" + api + "\n"
                + (texture.isBlank() ? "" : "texture=" + texture + "\n");
    }

    private static Path writeJar(Path jar, String metadata) {
        return writeJar(jar, metadata, Map.of());
    }

    private static Path writeJar(Path jar, String metadata, Map<String, byte[]> extra) {
        try {
            Files.createDirectories(jar.getParent());
            try (OutputStream output = Files.newOutputStream(jar);
                 JarOutputStream zip = new JarOutputStream(output)) {
                zip.putNextEntry(new JarEntry(metadata.stripLeading().startsWith("{")
                        ? "foabic.mod.json" : "foabic.mod"));
                zip.write(metadata.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                for (Map.Entry<String, byte[]> entry : extra.entrySet()) {
                    zip.putNextEntry(new JarEntry(entry.getKey()));
                    zip.write(entry.getValue());
                    zip.closeEntry();
                }
            }
            return jar;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Path tempDir() {
        try {
            return Files.createTempDirectory("foabic-test-");
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Path createDirectory(Path path) {
        try {
            return Files.createDirectories(path);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Mutable POJO used by the config round-trip test. */
    public static final class SampleConfig {
        public String name = "default";
        public int count = 3;

        public SampleConfig() {
        }
    }

    /** Record config used to verify Gson's record support. */
    public record RecordConfig(String name, int count) {
    }

    private static final class RecordingLogger implements System.Logger {

        private final List<String> errors = new ArrayList<>();

        @Override
        public String getName() {
            return "test";
        }

        @Override
        public boolean isLoggable(Level level) {
            return true;
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
            record(level, msg);
        }

        @Override
        public void log(Level level, ResourceBundle bundle, String format, Object... params) {
            record(level, format(format, params));
        }

        private void record(Level level, String message) {
            if (level == Level.ERROR) errors.add(message);
        }

        private String format(String message, Object[] params) {
            if (params == null || params.length == 0) return message;
            try {
                return java.text.MessageFormat.format(message, params);
            } catch (MissingResourceException e) {
                return message;
            }
        }
    }
}
