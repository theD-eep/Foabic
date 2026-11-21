package dev.foabic.fabric;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.fabric.registry.FabricRegistries;
import dev.foabic.loader.FoabicLoader;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, String> provided = new HashMap<>();
        loader.getAllMods().forEach(mod -> provided.put(
                mod.getMetadata().getId(),
                mod.getMetadata().getVersion().getFriendlyString()));

        FabricRegistries.bind();
        FabricNetwork.register();
        FoabicLoader.load(gameDirectory,
                // Preferred: foabic-mods. Also accept mods/ as a convenience
                // for users who drop Foabic jars next to Fabric mods; Fabric
                // ignores them as non-Fabric mods, while Foabic reads foabic.mod.json.
                List.of(gameDirectory.resolve("foabic-mods"), gameDirectory.resolve("mods")),
                environment, PLATFORM, provided);

        // All content has been submitted by now. Late submissions must fail
        // with FoabicRegistryFrozenException instead of a low-level crash.
        FoabicRegistries.freezeRegistration();

        // Signal bridges and creative-tab callbacks are registered after the
        // loader has resolved every Foabic mod. The generated resource pack is
        // registered before Fabric's first resource reload.
        FabricSignals.register();
        FabricCreativeTabs.register();
        FabricGeneratedResources.register(environment);

        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} attached to Fabric ({1}) with {2} Foabic mod(s)",
                Foabic.VERSION, environment, Foabic.mods().size());
    }
}
