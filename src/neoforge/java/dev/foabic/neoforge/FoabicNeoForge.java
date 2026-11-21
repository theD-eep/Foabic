package dev.foabic.neoforge;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.loader.FoabicLoader;
import dev.foabic.neoforge.registry.NeoForgeRegistries;
import dev.foabic.neoforge.registry.NeoForgeRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Path;
import java.util.List;

/** NeoForge adapter entrypoint. */
@Mod(FoabicNeoForge.MOD_ID)
public final class FoabicNeoForge {

    public static final String MOD_ID = "foabic";
    public static final String PLATFORM = "neoforge";
    private static final System.Logger LOG = System.getLogger("Foabic/neoforge");

    public FoabicNeoForge(IEventBus modBus, ModContainer container) {
        Environment environment = FMLEnvironment.getDist().isClient()
                ? Environment.CLIENT
                : Environment.SERVER;
        Path gameDirectory = FMLPaths.GAMEDIR.get();

        NeoForgeRegistries.bind();
        FoabicLoader.load(gameDirectory,
                List.of(gameDirectory.resolve("foabic-mods")),
                environment, PLATFORM);

        modBus.addListener(RegisterEvent.class, this::onRegister);
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, NeoForgeSignals::onServerStarted);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class,
                NeoForgeSignals::onRightClickBlock);

        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} attached to NeoForge ({1}) with {2} Foabic mod(s)",
                Foabic.VERSION, environment, Foabic.mods().size());
    }

    private void onRegister(RegisterEvent event) {
        RegisterEventAccessAdapter adapter = new RegisterEventAccessAdapter(event);
        NeoForgeRegistries.BLOCKS.flush(adapter);
        NeoForgeRegistries.ITEMS.flush(adapter);
    }

    private record RegisterEventAccessAdapter(RegisterEvent event)
            implements NeoForgeRegistry.RegisterEventAccess {

        @Override
        public net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>> registryKey() {
            return event.getRegistryKey();
        }

        @Override
        public <V> void register(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<V>> key,
                                 net.minecraft.resources.Identifier id,
                                 java.util.function.Supplier<V> value) {
            event.register(key, id, value);
        }
    }
}
