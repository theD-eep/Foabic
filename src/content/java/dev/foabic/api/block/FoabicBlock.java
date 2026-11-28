package dev.foabic.api.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A {@link BaseEntityBlock} that ticks its {@link FoabicBlockEntity} without
 * the mod writing a ticker lambda.
 *
 * <p>Subclasses still implement
 * {@link net.minecraft.world.level.block.EntityBlock#newBlockEntity}.
 */
public abstract class FoabicBlock extends BaseEntityBlock {

    protected FoabicBlock(Properties properties) {
        super(properties);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof FoabicBlockEntity foabic) {
                foabic.tick();
            }
        };
    }
}
