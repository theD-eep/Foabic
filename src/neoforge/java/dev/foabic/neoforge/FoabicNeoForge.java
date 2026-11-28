package dev.foabic.neoforge;

import dev.foabic.api.Foabic;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.loader.FoabicLoader;
import dev.foabic.neoforge.registry.NeoForgeRegistries;
import dev.foabic.neoforge.registry.NeoForgeRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        Map<String, String> provided = new HashMap<>();
        ModList.get().getMods().forEach(info ->
                provided.put(info.getModId(), info.getVersion().toString()));

        NeoForgeRegistries.bind();
        NeoForgeNetwork.register(modBus);
        FoabicLoader.load(gameDirectory,
                // Preferred: foabic-mods. Also accept mods/ as a convenience
                // for users who drop Foabic jars next to NeoForge mods.
                List.of(gameDirectory.resolve("foabic-mods"), gameDirectory.resolve("mods")),
                environment, PLATFORM, provided);

        modBus.addListener(RegisterEvent.class, this::onRegister);
        modBus.addListener(FMLCommonSetupEvent.class,
                event -> FoabicRegistries.freezeRegistration());
        NeoForgeCreativeTabs.register(modBus);
        modBus.addListener(AddPackFindersEvent.class, NeoForgeGeneratedResources::onAddPackFinders);
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, NeoForgeSignals::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class,
                NeoForgeSignals::onServerStopped);
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, NeoForgeSignals::onServerTick);
        NeoForge.EVENT_BUS.addListener(ServerChatEvent.class, NeoForgeSignals::onServerChat);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class,
                NeoForgeSignals::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(BreakBlockEvent.class, NeoForgeSignals::onBreakBlock);

        LOG.log(System.Logger.Level.INFO,
                "Foabic {0} attached to NeoForge ({1}) with {2} Foabic mod(s)",
                Foabic.VERSION, environment, Foabic.mods().size());
    }

    private void onRegister(RegisterEvent event) {
        RegisterEventAccessAdapter adapter = new RegisterEventAccessAdapter(event);
        NeoForgeRegistries.SOUND_EVENTS.flush(adapter);
        NeoForgeRegistries.BLOCKS.flush(adapter);
        NeoForgeRegistries.ITEMS.flush(adapter);
        NeoForgeRegistries.BLOCK_ENTITY_TYPES.flush(adapter);
        NeoForgeRegistries.ENTITY_TYPES.flush(adapter);
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
