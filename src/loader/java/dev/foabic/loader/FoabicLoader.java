package dev.foabic.loader;

import dev.foabic.api.Foabic;
import dev.foabic.api.internal.FoabicInternal;
import dev.foabic.api.internal.FoabicRuntime;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.mod.ModMetadata;
import dev.foabic.loader.mod.ModClassLoader;
import dev.foabic.loader.mod.ModContainerImpl;
import dev.foabic.loader.mod.ModContextImpl;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Discovers, resolves and initializes Foabic mods. One-shot per JVM. */
public final class FoabicLoader {

    private static final System.Logger LOG = System.getLogger("Foabic");
    private static final Set<String> PLATFORMS = Set.of("fabric", "neoforge");

    private FoabicLoader() {
    }

    public static FoabicRuntime load(Path gameDirectory, Path modsDirectory,
                                     Environment environment, String platform) {
        Objects.requireNonNull(modsDirectory, "modsDirectory");
        return load(gameDirectory, List.of(modsDirectory), environment, platform);
    }

    public static FoabicRuntime load(Path gameDirectory, List<Path> modsDirectories,
                                     Environment environment, String platform) {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(modsDirectories, "modsDirectories");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(platform, "platform");
        if (environment == Environment.UNIVERSAL) {
            throw new ModLoadException("loader environment must be CLIENT or SERVER");
        }
        if (!PLATFORMS.contains(platform)) {
            throw new ModLoadException("unsupported platform '" + platform
                    + "' (expected fabric or neoforge)");
        }
        if (FoabicInternal.isInstalled()) {
            throw new IllegalStateException("Foabic is already initialized");
        }

        Path gameDir = gameDirectory.toAbsolutePath().normalize();
        List<Path> modsDirs = modsDirectories.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .toList();
        Path stateRoot = gameDir.resolve(".foabic");
        long started = System.nanoTime();
        try {
            Files.createDirectories(stateRoot.resolve("data"));
        } catch (IOException e) {
            throw new ModLoadException("cannot create Foabic state directory " + stateRoot, e);
        }

        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} starting on {1}/{2}", Foabic.VERSION, platform, environment);
        List<DiscoveredMod> discovered = ModDiscoverer.discover(modsDirs, environment, LOG);
        List<DiscoveredMod> ordered = new DependencyResolver(discovered, LOG).resolve();

        URL[] urls = new URL[ordered.size()];
        try {
            for (int i = 0; i < ordered.size(); i++) {
                urls[i] = ordered.get(i).jar().toUri().toURL();
            }
        } catch (IOException e) {
            throw new ModLoadException("cannot build mod classpath", e);
        }

        ModClassLoader classLoader = new ModClassLoader(urls, FoabicLoader.class.getClassLoader());
        List<ModContainerImpl> containers = new ArrayList<>(ordered.size());
        for (DiscoveredMod mod : ordered) {
            ModMetadata metadata = mod.metadata();
            Path dataDirectory = stateRoot.resolve("data").resolve(metadata.id());
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                throw new ModLoadException("cannot create data directory for " + metadata.id(), e);
            }
            containers.add(new ModContainerImpl(metadata, mod.jar(), dataDirectory, classLoader));
        }

        List<ModContainer> publicContainers = List.copyOf(containers);
        LoaderRuntime runtime = new LoaderRuntime(platform, environment, gameDir, publicContainers);
        FoabicInternal.install(runtime);

        try {
            for (ModContainerImpl container : containers) {
                initialize(container, runtime);
            }
        } catch (Throwable failure) {
            FoabicInternal.uninstall(runtime);
            try {
                classLoader.close();
            } catch (IOException ignored) {
                // preserving the original failure
            }
            if (failure instanceof ModLoadException loadException) throw loadException;
            throw new ModLoadException("Foabic initialization failed", failure);
        }

        long millis = (System.nanoTime() - started) / 1_000_000L;
        LOG.log(System.Logger.Level.INFO,
                "Foabic loaded {0} mod(s) in {1} ms on {2}",
                containers.size(), millis, platform);
        return runtime;
    }

    private static void initialize(ModContainerImpl container, FoabicRuntime runtime) {
        ModMetadata metadata = container.metadata();
        ModContextImpl context = new ModContextImpl(runtime, container);
        LOG.log(System.Logger.Level.INFO, "initializing {0} {1}",
                metadata.id(), metadata.version());
        for (String entrypoint : metadata.entrypoints()) {
            try {
                Class<?> type = Class.forName(entrypoint, true, container.classLoader());
                if (!ModInitializer.class.isAssignableFrom(type)) {
                    throw new ModLoadException("mod '" + metadata.id() + "' entrypoint '"
                            + entrypoint + "' does not implement ModInitializer");
                }
                if (!Modifier.isPublic(type.getModifiers())) {
                    throw new ModLoadException("mod '" + metadata.id() + "' entrypoint '"
                            + entrypoint + "' must be public");
                }
                Constructor<?> constructor = type.getDeclaredConstructor();
                if (!Modifier.isPublic(constructor.getModifiers())) {
                    throw new ModLoadException("mod '" + metadata.id() + "' entrypoint '"
                            + entrypoint + "' needs a public no-argument constructor");
                }
                ModInitializer initializer = (ModInitializer) constructor.newInstance();
                initializer.onInitialize(context);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                throw new ModLoadException("mod '" + metadata.id() + "' entrypoint '"
                        + entrypoint + "' threw during initialization", cause);
            } catch (ModLoadException e) {
                throw e;
            } catch (ReflectiveOperationException | LinkageError e) {
                throw new ModLoadException("cannot initialize mod '" + metadata.id()
                        + "' entrypoint '" + entrypoint + "'", e);
            } catch (Throwable e) {
                throw new ModLoadException("mod '" + metadata.id() + "' entrypoint '"
                        + entrypoint + "' failed", e);
            }
        }
    }
}
