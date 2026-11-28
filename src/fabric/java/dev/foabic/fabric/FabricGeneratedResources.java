package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.loader.resource.GeneratedDataPack;
import dev.foabic.loader.resource.GeneratedResourcePack;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Registers Foabic's generated assets as an always-on Fabric built-in pack.
 *
 * <p>The pack is backed by a synthetic {@link ModContainer} rooted at
 * {@code .foabic/generated-resources}. Fabric's built-in-pack API accepts a
 * mod root, so this avoids Mixin and avoids writing into the Foabic jar.
 */
final class FabricGeneratedResources {

    private static final System.Logger LOG = System.getLogger("Foabic/fabric/resources");

    private FabricGeneratedResources() {
    }

    static void register(Environment environment) {
        if (environment != Environment.CLIENT) return;

        Path root = Foabic.gameDirectory()
                .resolve(".foabic")
                .resolve(GeneratedResourcePack.DIRECTORY);
        if (!GeneratedResourcePack.hasAssets(root)) {
            LOG.log(System.Logger.Level.DEBUG, "no generated Foabic assets to register");
            return;
        }
        registerPack(root, Identifier.fromNamespaceAndPath("foabic", "generated"),
                "Foabic generated block assets", "FOABIC_RESOURCE_PACK");
    }

    /**
     * Registers generated recipes, tags and loot. Fabric's built-in pack API
     * registers both CLIENT_RESOURCES and SERVER_DATA, so the same root can be
     * used on a dedicated server and on an integrated server.
     */
    static void registerData() {
        Path root = Foabic.gameDirectory()
                .resolve(".foabic")
                .resolve(GeneratedDataPack.DIRECTORY);
        if (!GeneratedDataPack.hasData(root)) {
            LOG.log(System.Logger.Level.DEBUG, "no generated Foabic data to register");
            return;
        }
        registerPack(root, Identifier.fromNamespaceAndPath("foabic", "generated_data"),
                "Foabic generated recipes, tags and loot", "FOABIC_DATA_PACK");
    }

    private static void registerPack(Path root, Identifier packId, String description,
                                     String logMarker) {
        Optional<ModContainer> foabic = FabricLoader.getInstance().getModContainer("foabic");
        if (foabic.isEmpty()) {
            LOG.log(System.Logger.Level.ERROR,
                    "cannot register {0}: Foabic's Fabric mod container is missing", packId);
            return;
        }

        ModContainer generated = new GeneratedModContainer(foabic.get(), root);
        boolean registered = ResourceLoader.registerBuiltinPack(
                packId,
                "",
                generated,
                Component.literal(description),
                PackActivationType.ALWAYS_ENABLED);
        if (registered) {
            LOG.log(System.Logger.Level.INFO,
                    "{0} platform=fabric id={1} root={2}", logMarker, packId, root);
        } else {
            LOG.log(System.Logger.Level.WARNING,
                    "Fabric rejected the Foabic pack {0} at {1}", packId, root);
        }
    }

    @SuppressWarnings("deprecation")
    private static final class GeneratedModContainer implements ModContainer {

        private final ModContainer delegate;
        private final Path root;

        private GeneratedModContainer(ModContainer delegate, Path root) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        }

        @Override
        public ModMetadata getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public List<Path> getRootPaths() {
            return List.of(root);
        }

        @Override
        public ModOrigin getOrigin() {
            return delegate.getOrigin();
        }

        @Override
        public Optional<ModContainer> getContainingMod() {
            return delegate.getContainingMod();
        }

        @Override
        public Collection<ModContainer> getContainedMods() {
            return List.of();
        }

        @Override
        public Path getRootPath() {
            return root;
        }

        @Override
        public Path getPath(String path) {
            Objects.requireNonNull(path, "path");
            Path resolved = root.resolve(path).normalize();
            if (!resolved.startsWith(root)) {
                throw new IllegalArgumentException("path escapes generated pack: " + path);
            }
            if (!Files.exists(resolved)) {
                throw new RuntimeException("no such path in generated pack: " + path);
            }
            return resolved;
        }
    }
}
