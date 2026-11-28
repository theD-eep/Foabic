package dev.foabic.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A block entity with a small, typed synchronised-state system.
 *
 * <pre>{@code
 * public final class CollectorBlockEntity extends FoabicBlockEntity {
 *     private final SyncedInt feathers = syncedInt("feathers", 0);
 *
 *     public CollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
 *         super(type, pos, state);
 *     }
 *
 *     @Override public void tick() {
 *         if (level != null && !level.isClientSide && level.getGameTime() % 200 == 0) {
 *             feathers.increment();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>{@link SyncedInt#set} writes the field into the block-entity update tag and
 * sends a block update to tracking clients. On the client, the normal
 * {@code ClientboundBlockEntityDataPacket} path calls {@link #loadAdditional}
 * and updates the same fields. Saving and loading use the vanilla
 * {@code ValueInput}/{@code ValueOutput} API, so no custom NBT code is needed.
 */
public abstract class FoabicBlockEntity extends BlockEntity {

    private final List<SyncedField> fields = new ArrayList<>();

    protected FoabicBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Declares an integer field; call once in the field initializer. */
    protected final SyncedInt syncedInt(String key, int defaultValue) {
        return add(new SyncedInt(this, key(key), defaultValue));
    }

    protected final SyncedLong syncedLong(String key, long defaultValue) {
        return add(new SyncedLong(this, key(key), defaultValue));
    }

    protected final SyncedFloat syncedFloat(String key, float defaultValue) {
        return add(new SyncedFloat(this, key(key), defaultValue));
    }

    protected final SyncedBoolean syncedBoolean(String key, boolean defaultValue) {
        return add(new SyncedBoolean(this, key(key), defaultValue));
    }

    protected final SyncedString syncedString(String key, String defaultValue) {
        return add(new SyncedString(this, key(key), defaultValue));
    }

    /** Saves all declared fields. Subclasses may extend and call super. */
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (SyncedField field : fields) field.write(output);
    }

    /** Loads all declared fields. Subclasses may extend and call super. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (SyncedField field : fields) field.read(input);
    }

    /**
     * Marks the block entity dirty and sends its update tag to every tracking
     * client. Safe to call on either side; only the server sends a packet.
     */
    public final void sync() {
        setChanged();
        if (level instanceof ServerLevel server && !level.isClientSide()) {
            BlockState state = getBlockState();
            server.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /** Called by {@link #ticker()} once per game tick on both sides. */
    public void tick() {
    }

    /**
     * A ticker for {@link FoabicBlock#getTicker}. Cast the return value to the
     * block's block-entity type if the compiler needs it.
     */
    public static <T extends FoabicBlockEntity> BlockEntityTicker<T> ticker() {
        return (level, pos, state, blockEntity) -> blockEntity.tick();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    private <T extends SyncedField> T add(T field) {
        fields.add(field);
        return field;
    }

    private static String key(String key) {
        Objects.requireNonNull(key, "key");
        if (key.isBlank()) throw new IllegalArgumentException("sync key must not be blank");
        return key;
    }
}
