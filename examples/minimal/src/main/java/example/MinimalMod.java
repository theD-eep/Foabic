package example;

import dev.foabic.api.event.FoabicSubscription;
import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.network.FoabicPacket;
import dev.foabic.api.network.NetworkManager;
import dev.foabic.api.registry.BlockEntityRegistration;
import dev.foabic.api.registry.BlockItemRegistration;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.registry.FoabicRegistryFrozenException;
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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The Foabic 1.2 acceptance mod.
 *
 * <p>It exercises content registration, the generated model path, all v1.1
 * signals, config loading, packet registration and explicit subscription
 * removal. The same jar runs on Fabric and NeoForge.
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

        context.resources().block(BLOCK_ID, "foabic_test:block/test_block");

        TestConfig config = context.config().register(TestConfig.class);
        config.greeting = "hello-v1.2";
        context.config().save(config);
        NetworkManager.register(StatusPacket.class, StatusPacket::new);

        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_REGISTER platform={0} block={1} item={1}",
                context.platform(), BLOCK_ID);
        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_V12_REGISTER platform={0} block_entity={1} sound={2}",
                context.platform(), ENTITY_ID, SOUND_ID);
        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_CONFIG platform={0} greeting={1}",
                context.platform(), config.greeting);
        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_NETWORK platform={0} packet={1}",
                context.platform(), StatusPacket.class.getName());

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
            try {
                context.submit(FoabicRegistries.ITEMS,
                        ResourceLocation.of("foabic_test", "late_item"), () -> null);
                context.logger().log(System.Logger.Level.ERROR,
                        "FOABIC_FROZEN_GUARD platform={0} caught=false", context.platform());
            } catch (FoabicRegistryFrozenException expected) {
                context.logger().log(System.Logger.Level.INFO,
                        "FOABIC_FROZEN_GUARD platform={0} caught=true object={1}",
                        context.platform(), expected.objectId());
            }
        });

        AtomicReference<FoabicSubscription> tickSubscription = new AtomicReference<>();
        tickSubscription.set(context.connect(ServerTickSignal.class, signal -> {
            if (signal.tick() == 20L) {
                context.logger().log(System.Logger.Level.INFO,
                        "FOABIC_TICK platform={0} tick={1} players={2}",
                        signal.platform(), signal.tick(), signal.playerCount());
            }
            if (signal.tick() == 40L) {
                FoabicSubscription subscription = tickSubscription.get();
                if (subscription != null) subscription.unsubscribe();
                context.logger().log(System.Logger.Level.INFO,
                        "FOABIC_UNSUBSCRIBE platform={0} tick={1}",
                        signal.platform(), signal.tick());
            }
        }));

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

    /** Mutable config with real defaults for the first run. */
    public static final class TestConfig {
        public String greeting = "hello";
        public int exampleNumber = 42;

        public TestConfig() {
        }
    }

    /** Registered with the network subsystem; the dedicated acceptance server has no peer. */
    public static final class StatusPacket implements FoabicPacket {
        private String text = "";

        public StatusPacket() {
        }

        @Override
        public byte[] encode() {
            return text.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void decode(byte[] data) {
            text = new String(data, StandardCharsets.UTF_8);
        }
    }

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

    public static final class TestBlockEntity extends BlockEntity {

        public TestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }
    }
}
