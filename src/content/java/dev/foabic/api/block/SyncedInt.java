package dev.foabic.api.block;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * An integer block-entity field that is saved to disk and sent to clients
 * whenever it changes.
 */
public final class SyncedInt implements SyncedField {

    private final FoabicBlockEntity owner;
    private final String key;
    private final int defaultValue;
    private int value;

    SyncedInt(FoabicBlockEntity owner, String key, int defaultValue) {
        this.owner = owner;
        this.key = key;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    public int get() {
        return value;
    }

    public void set(int value) {
        if (this.value == value) return;
        this.value = value;
        owner.sync();
    }

    public int add(int delta) {
        set(value + delta);
        return value;
    }

    public int increment() {
        return add(1);
    }

    public int defaultValue() {
        return defaultValue;
    }

    @Override
    public void write(ValueOutput output) {
        output.putInt(key, value);
    }

    @Override
    public void read(ValueInput input) {
        value = input.getIntOr(key, defaultValue);
    }
}
