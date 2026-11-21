package dev.foabic.api.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Objects;

/**
 * A block-entity type bound to one or more blocks.
 *
 * @param <T> the block-entity implementation type
 */
public final class BlockEntityRegistration<T extends BlockEntity> {

    private final Registration<BlockEntityType<?>> type;
    private final List<Registration<? extends Block>> blocks;

    public BlockEntityRegistration(Registration<BlockEntityType<?>> type,
                                   List<? extends Registration<? extends Block>> blocks) {
        this.type = Objects.requireNonNull(type, "type");
        this.blocks = List.copyOf(blocks);
    }

    public Registration<BlockEntityType<?>> type() {
        return type;
    }

    public List<Registration<? extends Block>> blocks() {
        return blocks;
    }

    /**
     * Returns the concrete block-entity type. Safe because the loader only
     * creates this registration with a factory of {@code T}.
     */
    @SuppressWarnings("unchecked")
    public BlockEntityType<T> get() {
        return (BlockEntityType<T>) type.get();
    }

    public boolean isPresent() {
        return type.isPresent();
    }
}
