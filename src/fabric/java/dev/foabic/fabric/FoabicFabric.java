package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.fabric.registry.FabricRegistries;
import dev.foabic.loader.FoabicLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

/**
 * Fabric entrypoint.
 *
 * <p>Foabic content mods are initialized on both client and server so the
 * vanilla registry sync sees identical block/item ids. Server-only signals are
 * registered only in a dedicated-server environment.
 */
public final class FoabicFabric implements ModInitializer {

    public static final String PLATFORM = "fabric";
    private static final System.Logger LOG = System.getLogger("Foabic/fabric");

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        Path gameDirectory = loader.getGameDir();
        Environment environment = loader.getEnvironmentType() == EnvType.CLIENT
                ? Environment.CLIENT
                : Environment.SERVER;

        FabricRegistries.bind();
        FoabicLoader.load(gameDirectory,
                List.of(gameDirectory.resolve("foabic-mods")),
                environment, PLATFORM);

        // ServerLifecycleEvents also covers an integrated server on a client.
        // The use-block bridge filters to the logical server, so there is no
        // double dispatch.
        FabricSignals.register();
        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} attached to Fabric ({1}) with {2} Foabic mod(s)",
                Foabic.VERSION, environment, Foabic.mods().size());
    }
}
