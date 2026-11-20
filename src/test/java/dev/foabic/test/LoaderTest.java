package dev.foabic.test;

import dev.foabic.api.Foabic;
import dev.foabic.api.config.Config;
import dev.foabic.api.event.ServerStartedEvent;
import dev.foabic.api.game.Player;
import dev.foabic.api.game.Server;
import dev.foabic.api.internal.FoabicInternal;
import dev.foabic.loader.FoabicLoader;
import dev.foabic.loader.ModLoadException;
import dev.foabic.test.fixture.TestMod;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

final class LoaderTest {

    private LoaderTest() {
    }

    static void run() {
        simpleLoad();
        dependencyOrder();
        missingDependency();
        duplicateId();
        environmentSkip();
        optionalMissing();
        apiMismatch();
    }

    private static void simpleLoad() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("testmod.jar", manifest("testmod", "1.2.3",
                    "dev.foabic.test.fixture.TestMod", ""),
                    Map.of("data/hello.txt", "hello from jar".getBytes(StandardCharsets.UTF_8)));
            FoabicLoader.load(fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER);

            Assertions.assertTrue(Foabic.isReady(), "runtime installed");
            Assertions.assertTrue(TestMod.initialized, "entrypoint ran");
            Assertions.assertEquals("hello from jar", TestMod.resourceText, "scoped resources");
            Assertions.assertEquals(1, Foabic.mods().size(), "one mod");
            Assertions.assertEquals("testmod", Foabic.mods().get(0).id(), "mod id");
            Assertions.assertEquals("1.2.3", Foabic.mods().get(0).version().toString(), "mod version");
            Assertions.assertEquals(1, Foabic.commands().roots().size(), "command buffered");

            Foabic.events().post(new ServerStartedEvent(new DummyServer(fixture.game)));
            Assertions.assertEquals(1, TestMod.STARTED.get(), "event delivered");

            Config config = TestMod.context.config();
            config.set("answer", 42);
            config.save();
            Config reloaded = Config.load(config.path());
            Assertions.assertEquals(42, reloaded.getInt("answer", -1), "config round trip");
        } finally {
            unload();
        }
    }

    private static void dependencyOrder() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("moda.jar", manifest("moda", "1.0.0",
                    "dev.foabic.test.fixture.ModA", ""), Map.of());
            fixture.jar("modb.jar", manifest("modb", "1.0.0",
                    "dev.foabic.test.fixture.ModB", "depends=moda@^1.0"), Map.of());
            FoabicLoader.load(fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER);
            Assertions.assertEquals(List.of("a", "b"), TestMod.ORDER, "dependency-first order");
        } finally {
            unload();
        }
    }

    private static void missingDependency() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("broken.jar", manifest("broken", "1.0.0",
                    "dev.foabic.test.fixture.TestMod", "depends=missing@^1.0"), Map.of());
            Assertions.assertThrows(ModLoadException.class,
                    () -> FoabicLoader.load(
                            fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER),
                    "missing dependency rejected");
            Assertions.assertFalse(Foabic.isReady(), "runtime not installed on failure");
        } finally {
            unload();
        }
    }

    private static void duplicateId() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("one.jar", manifest("same", "1.0.0",
                    "dev.foabic.test.fixture.ModA", ""), Map.of());
            fixture.jar("two.jar", manifest("same", "1.0.0",
                    "dev.foabic.test.fixture.ModB", ""), Map.of());
            Assertions.assertThrows(ModLoadException.class,
                    () -> FoabicLoader.load(
                            fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER),
                    "duplicate mod id rejected");
        } finally {
            unload();
        }
    }

    private static void environmentSkip() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("client.jar", manifest("clientmod", "1.0.0",
                    "dev.foabic.test.fixture.TestMod", "environment=client"), Map.of());
            FoabicLoader.load(fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER);
            Assertions.assertEquals(0, Foabic.mods().size(), "client mod skipped on server");
        } finally {
            unload();
        }
    }

    private static void optionalMissing() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("optional.jar", manifest("optionalmod", "1.0.0",
                            "dev.foabic.test.fixture.TestMod", "optional=missing@^1.0"),
                    Map.of("data/hello.txt", "optional".getBytes(StandardCharsets.UTF_8)));
            FoabicLoader.load(fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER);
            Assertions.assertEquals(1, Foabic.mods().size(), "optional dependency may be absent");
        } finally {
            unload();
        }
    }

    private static void apiMismatch() {
        try (Fixture fixture = Fixture.create()) {
            fixture.jar("future.jar", manifest("future", "1.0.0",
                    "dev.foabic.test.fixture.TestMod", "api=^9.0"), Map.of());
            Assertions.assertThrows(ModLoadException.class,
                    () -> FoabicLoader.load(
                            fixture.game, fixture.mods, dev.foabic.api.mod.Environment.SERVER),
                    "future API rejected");
        } finally {
            unload();
        }
    }

    private static void unload() {
        if (Foabic.isReady()) {
            FoabicInternal.uninstall(FoabicInternal.runtime());
        }
    }

    private static String manifest(String id, String version, String entrypoint, String extra) {
        return """
                id=%s
                name=%s
                version=%s
                entrypoint=%s
                description=test fixture
                authors=Tester
                %s
                """.formatted(id, id, version, entrypoint, extra);
    }

    private static final class Fixture implements AutoCloseable {
        private final Path root;
        private final Path game;
        private final Path mods;

        private Fixture(Path root) throws IOException {
            this.root = root;
            this.game = root.resolve("game");
            this.mods = game.resolve("mods");
            Files.createDirectories(mods);
        }

        private static Fixture create() {
            TestMod.reset();
            try {
                return new Fixture(Files.createTempDirectory("foabic-test"));
            } catch (IOException e) {
                throw new AssertionError("cannot create fixture", e);
            }
        }

        private void jar(String name, String manifest, Map<String, byte[]> resources) {
            Path jar = mods.resolve(name);
            try (OutputStream file = Files.newOutputStream(jar);
                 JarOutputStream out = new JarOutputStream(file)) {
                add(out, "foabic.mod", manifest.getBytes(StandardCharsets.UTF_8));
                for (Map.Entry<String, byte[]> entry : resources.entrySet()) {
                    add(out, entry.getKey(), entry.getValue());
                }
            } catch (IOException e) {
                throw new AssertionError("cannot write mod jar", e);
            }
        }

        private static void add(JarOutputStream out, String name, byte[] bytes) throws IOException {
            JarEntry entry = new JarEntry(name);
            out.putNextEntry(entry);
            out.write(bytes);
            out.closeEntry();
        }

        @Override
        public void close() {
            // Temp directories are intentionally kept small and are cleaned by
            // the operating system; deleting open jars on Windows is flaky.
        }
    }

    private static final class DummyServer implements Server {
        private final Path directory;

        private DummyServer(Path directory) {
            this.directory = directory;
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public String motd() {
            return "test";
        }

        @Override
        public String version() {
            return "26.1";
        }

        @Override
        public boolean dedicated() {
            return true;
        }

        @Override
        public long tick() {
            return 1;
        }

        @Override
        public int playerCount() {
            return 0;
        }

        @Override
        public List<Player> players() {
            return List.of();
        }

        @Override
        public Path directory() {
            return directory;
        }

        @Override
        public <T> T unwrap(Class<T> type) {
            throw new ClassCastException("dummy server");
        }
    }
}
