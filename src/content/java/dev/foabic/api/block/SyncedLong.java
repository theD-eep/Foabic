package dev.foabic.api.block;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A saved and client-synchronised long field. */
public final class SyncedLong implements SyncedField {

    private final FoabicBlockEntity owner;
    private final String key;
    private final long defaultValue;
    private long value;

    SyncedLong(FoabicBlockEntity owner, String key, long defaultValue) {
        this.owner = owner;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public long get() {
        return value;
    }

    public void set(long value) {
        if (this.value == value) return;
        this.value = value;
        owner.sync();
    }

    public long add(long delta) {
        set(value + delta);
        return value;
    }

    @Override
    public void write(ValueOutput output) {
        output.putLong(key, value);
    }

    @Override
    public void read(ValueInput input) {
        value = input.getLongOr(key, defaultValue);
    }
}
