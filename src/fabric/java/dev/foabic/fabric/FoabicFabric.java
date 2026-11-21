package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.loader.FoabicLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

/**
 * Fabric entrypoint. Foabic content mods are initialized on both client and
 * server so vanilla registry sync sees the same block/item IDs on both sides.
 * Server-only events and signals are registered only in a dedicated server
 * environment.
 */
public final class FoabicFabric implements ModInitializer {

    private static final System.Logger LOG = System.getLogger("Foabic/fabric");

    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        Path gameDirectory = loader.getGameDir();
        Environment environment = loader.getEnvironmentType() == EnvType.CLIENT
                ? Environment.CLIENT
                : Environment.SERVER;

        FoabicLoader.load(gameDirectory,
                List.of(gameDirectory.resolve("foabic-mods"), gameDirectory.resolve("mods")),
                environment);

        if (environment == Environment.SERVER) {
            FabricEvents.register();
        }
        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} attached to Fabric ({1}) with {2} Foabic mod(s)",
                Foabic.VERSION, environment, Foabic.mods().size());
    }
}
