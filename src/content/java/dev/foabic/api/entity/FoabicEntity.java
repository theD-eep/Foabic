package dev.foabic.api.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A small base class for custom entities with vanilla data synchronisation.
 *
 * <p>Fields declared with {@link #define} and stored through
 * {@link #setSynced} are sent automatically by {@code SynchedEntityData} to
 * every tracking client. This is the same mechanism vanilla uses for health,
 * pose and custom names; no Foabic packet is involved.
 *
 * <pre>{@code
 * public final class FeatherWisp extends FoabicEntity {
 *     private static final EntityDataAccessor<Integer> GLOW =
 *             define(FeatherWisp.class, EntityDataSerializers.INT);
 *
 *     public FeatherWisp(EntityType<?> type, Level level) {
 *         super(type, level);
 *     }
 *
 *     @Override protected void defineData(SynchedEntityData.Builder builder) {
 *         builder.define(GLOW, 0);
 *     }
 *
 *     public void setGlow(int value) { setSynced(GLOW, value); }
 * }
 * }</pre>
 */
public abstract class FoabicEntity extends Entity {

    protected FoabicEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    /**
     * Declares a synced-data accessor. Call from a static field initializer on
     * the concrete entity class.
     */
    protected static <T> EntityDataAccessor<T> define(
            Class<? extends FoabicEntity> owner, EntityDataSerializer<T> serializer) {
        return SynchedEntityData.defineId(owner, serializer);
    }

    /** Defines the default values. Override in the concrete entity. */
    @Override
    protected final void defineSynchedData(SynchedEntityData.Builder builder) {
        defineData(builder);
    }

    /** Subclasses define their accessors and default values here. */
    protected abstract void defineData(SynchedEntityData.Builder builder);

    /** Sets a synced value; clients receive it on the next tracker update. */
    protected final <T> void setSynced(EntityDataAccessor<T> accessor, T value) {
        getEntityData().set(accessor, value);
    }

    protected final <T> T synced(EntityDataAccessor<T> accessor) {
        return getEntityData().get(accessor);
    }

    /** Subclasses with persistent custom fields override this. */
    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    /** Subclasses with persistent custom fields override this. */
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    /** Non-living entities are not damageable by default. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }
}
