package smoke;

import dev.foabic.api.event.LevelTickEvent;
import dev.foabic.api.event.PlayerJoinEvent;
import dev.foabic.api.event.ServerStartedEvent;
import dev.foabic.api.event.ServerStartingEvent;
import dev.foabic.api.event.ServerStoppedEvent;
import dev.foabic.api.event.ServerStoppingEvent;
import dev.foabic.api.event.ServerTickEvent;
import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.ResourceLocation;
import dev.foabic.fabric.registry.FabricRegistries;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SmokeMod implements ModInitializer {
    @Override
    public void onInitialize(ModContext context) {
        System.out.println("FOABIC_SMOKE init " + context.mod().id() + " api=" + dev.foabic.api.Foabic.VERSION);

        ResourceLocation blockId = ResourceLocation.of("smoketest", "test_block");
        Identifier mcBlockId = Identifier.fromNamespaceAndPath(blockId.namespace(), blockId.path());
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, mcBlockId);
        Block block = new Block(BlockBehaviour.Properties.of()
                .setId(blockKey)
                .strength(1.0F));
        context.submit(FabricRegistries.BLOCKS, blockId, block);

        Identifier mcItemId = Identifier.fromNamespaceAndPath("smoketest", "test_block");
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, mcItemId);
        BlockItem item = new BlockItem(block, new Item.Properties().setId(itemKey));
        context.submit(FabricRegistries.ITEMS, ResourceLocation.of("smoketest", "test_block"), item);

        System.out.println("FOABIC_REGISTRY block=" + FabricRegistries.BLOCKS.contains(blockId)
                + " item=" + FabricRegistries.ITEMS.contains(ResourceLocation.of("smoketest", "test_block"))
                + " blocks=" + FabricRegistries.BLOCKS.size()
                + " items=" + FabricRegistries.ITEMS.size());
        System.out.println("FOABIC_ASSET=" + net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("smoketest")
                .flatMap(mod -> mod.findPath("assets/smoketest/blockstates/test_block.json"))
                .isPresent());

        context.connect(UseBlockSignal.class, signal -> {
            System.out.println("FOABIC_USE_SIGNAL block=" + signal.block()
                    + " pos=" + signal.pos() + " hand=" + signal.hand());
            try {
                signal.player().sendActionBar("Foabic: Test Block used");
            } catch (Throwable ignored) {
                // Fake players used by the server test have no client connection.
            }
            signal.setResult(UseBlockSignal.Result.SUCCESS);
        });

        context.connect(PlayerJoinEvent.class, event -> {
            net.minecraft.server.level.ServerPlayer player =
                    event.player().unwrap(net.minecraft.server.level.ServerPlayer.class);
            net.minecraft.core.BlockPos pos = player.blockPosition().offset(2, 1, 0);
            player.level().setBlockAndUpdate(pos, block.defaultBlockState());
            System.out.println("FOABIC_PLACED " + pos);
        });

        context.connect(ServerStartingEvent.class,
                event -> System.out.println("FOABIC_SMOKE starting name=" + event.server().name()));
        context.connect(ServerStartedEvent.class,
                event -> System.out.println("FOABIC_SMOKE started version=" + event.server().version()));
        context.connect(ServerTickEvent.class, event -> {
            if (event.tick() <= 3) System.out.println("FOABIC_SMOKE tick=" + event.tick());
        });
        context.connect(LevelTickEvent.class, event -> {
            if (event.tick() <= 2) {
                System.out.println("FOABIC_SMOKE level=" + event.level().dimension());
            }
        });
        context.connect(ServerStoppingEvent.class,
                event -> System.out.println("FOABIC_SMOKE stopping"));
        context.connect(ServerStoppedEvent.class,
                event -> System.out.println("FOABIC_SMOKE stopped"));

        var test = context.commands().literal("foabictest");
        test.executes(command -> {
            command.source().sendMessage("FOABIC_COMMAND_OK from=" + command.source().name());
            return 1;
        });
        test.argument("text", dev.foabic.api.command.ArgumentType.STRING)
                .suggests(command -> java.util.List.of("alpha", "beta"))
                .executes(command -> {
                    command.source().sendMessage("FOABIC_ARG_OK=" + command.getString("text"));
                    return 1;
                });
        test.literal("use").executes(command -> {
            var source = command.source().unwrap(net.minecraft.commands.CommandSourceStack.class);
            ServerLevel level = source.getLevel();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, 64, 0);
            level.setBlockAndUpdate(pos, block.defaultBlockState());
            FakePlayer fake = FakePlayer.get(level);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(pos), Direction.UP, pos, false);
            InteractionResult result = UseBlockCallback.EVENT.invoker().interact(
                    fake, level, InteractionHand.MAIN_HAND, hit);
            command.source().sendMessage("FOABIC_USE_RESULT=" + result);
            return 1;
        });
    }
}