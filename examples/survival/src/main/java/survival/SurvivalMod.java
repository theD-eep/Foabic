package survival;

import dev.foabic.api.block.FoabicBlock;
import dev.foabic.api.block.FoabicBlockEntity;
import dev.foabic.api.block.SyncedInt;
import dev.foabic.api.entity.FoabicEntity;
import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.registry.BlockEntityRegistration;
import dev.foabic.api.registry.BlockItemRegistration;
import dev.foabic.api.registry.CreativeTab;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Foabic 1.3 survival example: a placeable, ticking block entity, a real sword
 * item, a custom entity type, and a data provider that writes recipes, tags,
 * loot, models and language files.
 */
public final class SurvivalMod implements ModInitializer {

    public static final ResourceLocation COLLECTOR =
            ResourceLocation.of("foabic_survival", "feather_collector");
    public static final ResourceLocation COLLECTOR_BE =
            ResourceLocation.of("foabic_survival", "feather_collector");
    public static final ResourceLocation BLADE =
            ResourceLocation.of("foabic_survival", "feather_blade");
    public static final ResourceLocation WISP =
            ResourceLocation.of("foabic_survival", "feather_wisp");
    public static final ResourceLocation SWORDS =
            ResourceLocation.parse("minecraft:swords");

    @Override
    public void onInitialize(ModContext context) {
        AtomicReference<BlockEntityRegistration<CollectorBlockEntity>> entityRef =
                new AtomicReference<>();

        BlockItemRegistration collector = context.submitBlock(COLLECTOR,
                () -> new CollectorBlock(BlockBehaviour.Properties.of()
                        .setId(ResourceKey.create(Registries.BLOCK, mcId(COLLECTOR)))
                        .strength(2.0F)
                        .requiresCorrectToolForDrops(),
                        () -> entityRef.get().get()));

        BlockEntityRegistration<CollectorBlockEntity> blockEntity =
                context.submitBlockEntity(COLLECTOR_BE, collector.block(),
                        (pos, state) -> new CollectorBlockEntity(entityRef.get().get(), pos, state));
        entityRef.set(blockEntity);

        context.submitItem(BLADE,
                properties -> new Item(properties.sword(ToolMaterial.WOOD, 3.0F, -2.4F)),
                CreativeTab.COMBAT);

        context.submitEntity(WISP,
                EntityType.Builder.of(FeatherWisp::new, MobCategory.MISC)
                        .sized(0.5F, 0.5F)
                        .clientTrackingRange(8));

        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_SURVIVAL_READY platform={0} block={1} block_entity={2} item={3} entity={4}",
                context.platform(), COLLECTOR, COLLECTOR_BE, BLADE, WISP);

        context.connect(ServerStartedSignal.class, signal -> {
            if (signal.server() == null) return;
            ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(
                    Registries.RECIPE, mcId(COLLECTOR));
            boolean recipeLoaded = signal.server().getRecipeManager().byKey(recipeKey).isPresent();
            boolean blockEntityPresent =
                    FoabicRegistries.BLOCK_ENTITY_TYPES.get(COLLECTOR_BE).isPresent();
            boolean itemPresent = FoabicRegistries.ITEMS.get(BLADE).isPresent();
            boolean entityPresent = FoabicRegistries.ENTITY_TYPES.get(WISP).isPresent();
            boolean swordTag = signal.server().registryAccess()
                    .lookupOrThrow(Registries.ITEM)
                    .get(mcId(BLADE))
                    .map(holder -> holder.is(TagKey.create(Registries.ITEM, mcId(SWORDS))))
                    .orElse(false);
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_SURVIVAL_SERVER platform={0} recipe_loaded={1} "
                            + "block_entity={2} item={3} entity={4} sword_tag={5}",
                    context.platform(), recipeLoaded, blockEntityPresent, itemPresent,
                    entityPresent, swordTag);
        });
    }

    static Identifier mcId(ResourceLocation id) {
        return Identifier.fromNamespaceAndPath(id.namespace(), id.path());
    }

    /** A block that ticks its block entity through FoabicBlock. */
    public static final class CollectorBlock extends FoabicBlock {

        private final Supplier<BlockEntityType<CollectorBlockEntity>> type;

        public CollectorBlock(Properties properties,
                              Supplier<BlockEntityType<CollectorBlockEntity>> type) {
            super(properties);
            this.type = type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CollectorBlockEntity(type.get(), pos, state);
        }
    }

    /** Increments a saved, client-synchronised counter every ten seconds. */
    public static final class CollectorBlockEntity extends FoabicBlockEntity {

        private final SyncedInt feathers = syncedInt("feathers", 0);

        public CollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        @Override
        public void tick() {
            if (level == null || level.isClientSide()) return;
            if (level.getGameTime() % 200L == 0L) {
                feathers.increment();
            }
        }

        public int feathers() {
            return feathers.get();
        }
    }

    /** A tiny custom entity with one vanilla-synced integer. */
    public static final class FeatherWisp extends FoabicEntity {

        private static final EntityDataAccessor<Integer> CHARGE =
                define(FeatherWisp.class, EntityDataSerializers.INT);

        public FeatherWisp(EntityType<?> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineData(SynchedEntityData.Builder builder) {
            builder.define(CHARGE, 0);
        }

        public int charge() {
            return synced(CHARGE);
        }

        public void setCharge(int value) {
            setSynced(CHARGE, value);
        }
    }
}
