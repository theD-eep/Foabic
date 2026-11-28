package dev.foabic.loader.mod;

import dev.foabic.api.config.ConfigManager;
import dev.foabic.api.data.FoabicDataContext;
import dev.foabic.api.data.internal.FoabicDataContextImpl;
import dev.foabic.api.event.EventBus;
import dev.foabic.api.internal.FoabicRuntime;
import dev.foabic.api.mod.Environment;
import dev.foabic.api.mod.ModContainer;
import dev.foabic.api.mod.ModContext;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.registry.Registration;
import dev.foabic.api.registry.Registry;
import dev.foabic.api.resource.ModResources;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.loader.config.ConfigManagerImpl;
import dev.foabic.loader.resource.BlockModelRequest;
import dev.foabic.loader.resource.ItemModelRequest;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The loader's per-mod context. It also acts as the mod's generated-resource
 * sink and records every block submitted through this context.
 */
public final class ModContextImpl implements ModContext, ModResources {

    private final FoabicRuntime runtime;
    private final ModContainerImpl container;
    private final ConfigManager config;
    private final FoabicDataContextImpl data;
    private final Set<ResourceLocation> registeredBlocks = new LinkedHashSet<>();
    private final Map<ResourceLocation, BlockModelRequest> blockRequests = new LinkedHashMap<>();
    private final Map<ResourceLocation, ItemModelRequest> itemRequests = new LinkedHashMap<>();

    public ModContextImpl(FoabicRuntime runtime, ModContainerImpl container) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.container = Objects.requireNonNull(container, "container");
        this.config = new ConfigManagerImpl(runtime.gameDirectory(), container.id(),
                container.logger());
        this.data = new FoabicDataContextImpl(container.id());
    }

    @Override public ModContainer mod() { return container; }
    @Override public EventBus events() { return runtime.events(); }
    @Override public Environment environment() { return container.environment(); }
    @Override public String platform() { return runtime.platform(); }
    @Override public System.Logger logger() { return container.logger(); }
    @Override public ConfigManager config() { return config; }
    @Override public FoabicDataContext data() { return data; }

    /** The loader-side collector, including file introspection. */
    public FoabicDataContextImpl dataContext() { return data; }

    @Override
    public <T> Registration<T> submit(Registry<T> registry, ResourceLocation id,
                                      Supplier<? extends T> factory) {
        FoabicRegistries.checkOpen(container.id(), id, logger());
        Registration<T> registration = ModContext.super.submit(registry, id, factory);
        if (registry == FoabicRegistries.BLOCKS) {
            registeredBlocks.add(Objects.requireNonNull(id, "id"));
        }
        return registration;
    }

    @Override
    public ModResources resources() {
        return this;
    }

    @Override
    public void block(ResourceLocation blockId, String texture, String modelParent) {
        BlockModelRequest request = new BlockModelRequest(blockId, texture, modelParent);
        blockRequests.put(request.block(), request);
    }

    @Override
    public void item(ResourceLocation itemId, ResourceLocation model) {
        ItemModelRequest request = new ItemModelRequest(itemId, model);
        itemRequests.put(request.item(), request);
    }

    public Set<ResourceLocation> registeredBlocks() {
        return Set.copyOf(registeredBlocks);
    }

    public List<BlockModelRequest> blockRequests() {
        return List.copyOf(blockRequests.values());
    }

    public List<ItemModelRequest> itemRequests() {
        return List.copyOf(itemRequests.values());
    }

    public String defaultTexture() {
        return container.metadata().texture();
    }
}
