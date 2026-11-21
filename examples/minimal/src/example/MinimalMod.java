package example;

import dev.foabic.api.mod.ModContext;
import dev.foabic.api.mod.ModInitializer;
import dev.foabic.api.registry.FoabicRegistries;
import dev.foabic.api.registry.Registration;
import dev.foabic.api.signal.ServerStartedSignal;
import dev.foabic.api.signal.UseBlockSignal;
import dev.foabic.api.util.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The Foabic 1.0 acceptance mod.
 *
 * <p>It registers one block and one block item, connects to the deterministic
 * startup signal, and connects to the block-use signal. The same source and
 * the same jar run on Fabric and NeoForge. Content factories are evaluated in
 * the platform's registration window; the item factory uses the block handle
 * instead of a pre-built block object.
 */
public final class MinimalMod implements ModInitializer {

    private static final ResourceLocation BLOCK_ID =
            ResourceLocation.of("foabic_test", "test_block");
    private static final Identifier MC_ID =
            Identifier.fromNamespaceAndPath("foabic_test", "test_block");

    @Override
    public void onInitialize(ModContext context) {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MC_ID);
        Registration<Block> block = context.submit(FoabicRegistries.BLOCKS, BLOCK_ID,
                () -> new Block(BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .strength(1.0F)));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, MC_ID);
        context.submit(FoabicRegistries.ITEMS, BLOCK_ID,
                () -> new BlockItem(block.get(), new Item.Properties().setId(itemKey)));

        context.logger().log(System.Logger.Level.INFO,
                "FOABIC_REGISTER platform={0} block={1} item={1}",
                context.platform(), BLOCK_ID);

        context.connect(ServerStartedSignal.class, signal -> {
            boolean blockPresent = FoabicRegistries.BLOCKS.get(BLOCK_ID).isPresent();
            boolean itemPresent = FoabicRegistries.ITEMS.get(BLOCK_ID).isPresent();
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_SIGNAL name=server_started platform={0} mc={1} mods={2}",
                    signal.platform(), signal.minecraftVersion(), signal.modCount());
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_STATUS platform={0} block_registered={1} item_registered={2}",
                    context.platform(), blockPresent, itemPresent);
        });

        context.connect(UseBlockSignal.class, signal -> {
            context.logger().log(System.Logger.Level.INFO,
                    "FOABIC_USE_SIGNAL player={0} block={1} pos={2} hand={3}",
                    signal.playerName(), signal.block(), signal.pos(), signal.hand());
            signal.setResult(UseBlockSignal.Result.SUCCESS);
        });
    }
}
