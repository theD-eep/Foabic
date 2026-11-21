package example;

import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.registry.BlockEntityRegistration;
import dev.foabic.api.registry.BlockItemRegistration;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.registry.Registration;
import dev.foabic.api.signal.PlayerBreakBlockSignal;
import dev.foabic.api.signal.PlayerInteractBlockSignal;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.signal.ServerTickSignal;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The Foabic 1.1 acceptance mod.
 *
 * <p>It exercises the expanded content surface (block + automatic BlockItem,
 * BlockEntity and SoundEvent), the generated block model path, and all three
 * new signals. The same jar runs on Fabric and NeoForge; content factories are
 * evaluated in each platform's registration window.
 */
public final class MinimalMod implements ModInitializer {

    private static final ResourceLocation BLOCK_ID =
            ResourceLocation.of("foabic_test", "test_block");
    private static final ResourceLocation ENTITY_ID =
            ResourceLocation.of("foabic_test", "test_block_entity");
    private static final ResourceLocation SOUND_ID =
            ResourceLocation.of("foabic_test", "test_sound");
    private static final Identifier MC_ID =
            Identifier.fromNamespaceAndPath("foabic_test", "test_block");

    @Override
    public void onInitialize(ModContext context) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MC_ID);

        // The block holds a supplier, not a value: the block is constructed
        // before the block-entity type exists on both platforms.
        AtomicReference<BlockEntityRegistration<TestBlockEntity>> entityRef =
                new AtomicReference<>();

        BlockItemRegistration block = context.submitBlock(BLOCK_ID,
                () -> new TestBlock(BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .strength(1.0F), () -> entityRef.get().get()));

        BlockEntityRegistration<TestBlockEntity> entity = context.submitBlockEntity(
                ENTITY_ID,
                block.block(),
                (pos, state) -> new TestBlockEntity(entityRef.get().get(), pos, state));
        entityRef.set(entity);

        Registration<SoundEvent> sound = context.submit(
                FoabicRegistries.SOUND_EVENTS,
                SOUND_ID,
                () -> SoundEvent.createVariableRangeEvent(
                        Identifier.fromNamespaceAndPath("foabic_test", "test_sound")));

        // The metadata already declares texture=foabic_test:block/test_block.
        // This one-liner is the code path for a per-block override.
        context.resources().block(BLOCK_ID, "foabic_test:block/test_block");

        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_REGISTER platform={0} block={1} item={1}",
                context.platform(), BLOCK_ID);
        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_V11_REGISTER platform={0} block_entity={1} sound={2}",
                context.platform(), ENTITY_ID, SOUND_ID);

        context.connect(ServerStartedSignal.class, signal -> {
            boolean blockPresent = FoabicRegistries.BLOCKS.get(BLOCK_ID).isPresent();
            boolean itemPresent = FoabicRegistries.ITEMS.get(BLOCK_ID).isPresent();
            boolean entityPresent = FoabicRegistries.BLOCK_ENTITY_TYPES.get(ENTITY_ID).isPresent();
            boolean soundPresent = FoabicRegistries.SOUND_EVENTS.get(SOUND_ID).isPresent();
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_SIGNAL name=server_started platform={0} mc={1} mods={2}",
                    signal.platform(), signal.minecraftVersion(), signal.modCount());
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_STATUS platform={0} block_registered={1} item_registered={2} "
                            + "block_entity_registered={3} sound_registered={4}",
                    context.platform(), blockPresent, itemPresent, entityPresent, soundPresent);
        });

        context.connect(ServerTickSignal.class, signal -> {
            if (signal.tick() == 20L) {
                context.logger().log(System.Logger.Level.INFO,
                        "FOABIC_TICK platform={0} tick={1} players={2}",
                        signal.platform(), signal.tick(), signal.playerCount());
            }
        });

        context.connect(PlayerInteractBlockSignal.class, signal -> {
            if (!signal.block().equals(BLOCK_ID)) return;
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_INTERACT player={0} block={1} pos={2} hand={3}",
                    signal.playerName(), signal.block(), signal.pos(), signal.hand());
            signal.setResult(PlayerInteractBlockSignal.Result.SUCCESS);
        });

        context.connect(PlayerBreakBlockSignal.class, signal -> {
            if (!signal.block().equals(BLOCK_ID)) return;
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_BREAK player={0} block={1} pos={2}",
                    signal.playerName(), signal.block(), signal.pos());
        });

        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_READY platform={0} sound={1}",
                context.platform(), sound.id());
    }

    /** A minimal block with a block entity. */
    public static final class TestBlock extends BaseEntityBlock {

        private final Supplier<BlockEntityType<TestBlockEntity>> type;

        public TestBlock(Properties properties, Supplier<BlockEntityType<TestBlockEntity>> type) {
            super(properties);
            this.type = type;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TestBlockEntity(type.get(), pos, state);
        }
    }

    /** A no-data block entity used to prove the block/type binding. */
    public static final class TestBlockEntity extends BlockEntity {

        public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
