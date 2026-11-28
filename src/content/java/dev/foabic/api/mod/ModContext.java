package dev.foabic.api.mod;

import dev.foabic.api.data.FoabicDataContext;
import dev.foabic.api.registry.BlockEntityRegistration;
import dev.foabic.api.registry.BlockItemRegistration;
import dev.foabic.api.registry.CreativeTab;
import dev.foabic.api.registry.FoabicCreativeTabs;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.registry.Registration;
import dev.foabic.api.registry.Registry;
import dev.foabic.api.resource.ModResources;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The per-mod handle passed to {@link ModInitializer#onInitialize(ModContext)}.
 *
 * <p>Lifecycle, event and configuration methods come from
 * {@link FoabicContext}. This interface adds the Minecraft content helpers.
 */
public interface ModContext extends FoabicContext {

    Environment environment();

    /**
     * Registers one object in a Foabic registry view.
     *
     * <p>Use {@link FoabicRegistries#BLOCKS}, {@link FoabicRegistries#ITEMS},
     * {@link FoabicRegistries#BLOCK_ENTITY_TYPES} or
     * {@link FoabicRegistries#SOUND_EVENTS}. The factory is evaluated during
     * the platform's registration window. Pass {@link Registration#get()} to a
     * later factory when one object needs another.
     *
     * @throws dev.foabic.api.registry.FoabicRegistryFrozenException if the
     *         registration window has already closed
     */
    default <T> Registration<T> submit(Registry<T> registry, ResourceLocation id,
                                       Supplier<? extends T> factory) {
        return registry.register(id, factory);
    }

    /**
     * Registers a block and its {@link BlockItem} under the same id, and adds
     * the item to the Building Blocks creative tab.
     *
     * <p>The block and item factories are evaluated in the platform
     * registration window; the item factory calls the block handle, never a
     * pre-built block.
     */
    default BlockItemRegistration submitBlock(ResourceLocation id,
                                              Supplier<? extends Block> blockFactory) {
        return submitBlock(id, blockFactory, block -> defaultBlockItem(block, id));
    }

    /** Registers a block and a custom item; the item factory receives the block. */
    default BlockItemRegistration submitBlock(ResourceLocation id,
                                              Supplier<? extends Block> blockFactory,
                                              Function<Block, ? extends Item> itemFactory) {
        return submitBlock(id, blockFactory, itemFactory, CreativeTab.BUILDING_BLOCKS);
    }

    /** Registers a block and a custom item, adding the item to {@code tab}. */
    default BlockItemRegistration submitBlock(ResourceLocation id,
                                              Supplier<? extends Block> blockFactory,
                                              Function<Block, ? extends Item> itemFactory,
                                              CreativeTab tab) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blockFactory, "blockFactory");
        Objects.requireNonNull(itemFactory, "itemFactory");
        Objects.requireNonNull(tab, "tab");

        Registration<Block> block = submit(FoabicRegistries.BLOCKS, id, blockFactory);
        Registration<Item> item = submit(FoabicRegistries.ITEMS, id,
                () -> itemFactory.apply(block.get()));
        FoabicCreativeTabs.add(tab, item);
        return new BlockItemRegistration(block, item);
    }

    /**
     * Registers a block-entity type bound to one block.
     *
     * <p>The type factory is evaluated after the block registration on both
     * platforms. Construct the block's {@code newBlockEntity} with
     * {@link BlockEntityRegistration#get()} from a later call, not during mod
     * initialization.
     */
    default <T extends BlockEntity> BlockEntityRegistration<T> submitBlockEntity(
            ResourceLocation id,
            Registration<? extends Block> block,
            BlockEntityType.BlockEntitySupplier<T> factory) {
        return submitBlockEntity(id, List.of(block), factory);
    }

    /** Registers a block-entity type bound to several blocks. */
    default <T extends BlockEntity> BlockEntityRegistration<T> submitBlockEntity(
            ResourceLocation id,
            List<? extends Registration<? extends Block>> blocks,
            BlockEntityType.BlockEntitySupplier<T> factory) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(factory, "factory");
        if (blocks.isEmpty()) {
            throw new IllegalArgumentException("block entity " + id + " needs at least one block");
        }
        List<Registration<? extends Block>> copy = List.copyOf(blocks);
        Registration<BlockEntityType<?>> type = submit(
                FoabicRegistries.BLOCK_ENTITY_TYPES, id,
                () -> new BlockEntityType<>(factory,
                        copy.stream().map(Registration::get).collect(Collectors.toSet())));
        return new BlockEntityRegistration<>(type, copy);
    }

    /**
     * Registers a custom entity type. The builder's factory is evaluated during
     * the platform registration window on NeoForge, so do not call
     * {@link Registration#get()} in the same initialization statement.
     */
    @SuppressWarnings("unchecked")
    default <T extends Entity> Registration<EntityType<T>> submitEntity(
            ResourceLocation id, EntityType.Builder<T> builder) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(builder, "builder");
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, mcId(id));
        Registration<EntityType<?>> raw = submit(FoabicRegistries.ENTITY_TYPES, id,
                () -> builder.build(key));
        return (Registration<EntityType<T>>) (Registration<?>) raw;
    }

    /** Registers a custom item and adds it to a vanilla creative tab. */
    default Registration<Item> submitItem(ResourceLocation id,
                                          Function<Item.Properties, ? extends Item> factory,
                                          CreativeTab tab) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(tab, "tab");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, mcId(id));
        Registration<Item> item = submit(FoabicRegistries.ITEMS, id,
                () -> factory.apply(new Item.Properties().setId(key)));
        FoabicCreativeTabs.add(tab, item);
        return item;
    }

    /** Registers a plain item and adds it to a vanilla creative tab. */
    default Registration<Item> submitItem(ResourceLocation id, CreativeTab tab) {
        return submitItem(id, Item::new, tab);
    }

    /** Adds an already-registered item to a vanilla creative tab. */
    default void addToCreativeTab(CreativeTab tab, Registration<? extends Item> item) {
        FoabicCreativeTabs.add(tab, item);
    }

    /** One-line generated model/blockstate API for this mod. */
    default ModResources resources() {
        return ModResources.noop();
    }

    /**
     * Records data-generation requests (blockstates, recipes, tags, loot,
     * language) for this mod. The loader writes them into a standard data pack
     * at {@code .foabic/generated-data}. A provider class named by
     * {@code data} in {@code foabic.mod.json} is invoked at build time by the
     * Foabic Gradle plugin.
     */
    FoabicDataContext data();

    private static Item defaultBlockItem(Block block, ResourceLocation id) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, mcId(id));
        return new BlockItem(block, new Item.Properties().setId(key));
    }

    private static Identifier mcId(ResourceLocation id) {
        return Identifier.fromNamespaceAndPath(id.namespace(), id.path());
    }
}
