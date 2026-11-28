package dev.foabic.api.block;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A saved and client-synchronised float field. */
public final class SyncedFloat implements SyncedField {

    private final FoabicBlockEntity owner;
    private final String key;
    private final float defaultValue;
    private float value;

    SyncedFloat(FoabicBlockEntity owner, String key, float defaultValue) {
        this.owner = owner;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public float get() {
        return value;
    }

    public void set(float value) {
        if (Float.compare(this.value, value) == 0) return;
        this.value = value;
        owner.sync();
    }

    @Override
    public void write(ValueOutput output) {
        output.putFloat(key, value);
    }

    @Override
    public void read(ValueInput input) {
        value = input.getFloatOr(key, defaultValue);
    }
}
