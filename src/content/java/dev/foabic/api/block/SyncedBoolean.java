package dev.foabic.api.block;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A saved and client-synchronised boolean field. */
public final class SyncedBoolean implements SyncedField {

    private final FoabicBlockEntity owner;
    private final String key;
    private final boolean defaultValue;
    private boolean value;

    SyncedBoolean(FoabicBlockEntity owner, String key, boolean defaultValue) {
        this.owner = owner;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public boolean get() {
        return value;
    }

    public void set(boolean value) {
        if (this.value == value) return;
        this.value = value;
        owner.sync();
    }

    @Override
    public void write(ValueOutput output) {
        output.putBoolean(key, value);
    }

    @Override
    public void read(ValueInput input) {
        value = input.getBooleanOr(key, defaultValue);
    }
}
