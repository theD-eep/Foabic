package dev.foabic.api.block;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Objects;

/** A saved and client-synchronised string field. */
public final class SyncedString implements SyncedField {

    private final FoabicBlockEntity owner;
    private final String key;
    private final String defaultValue;
    private String value;

    SyncedString(FoabicBlockEntity owner, String key, String defaultValue) {
        this.owner = owner;
        this.key = key;
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.value = defaultValue;
    }

    public String get() {
        return value;
    }

    public void set(String value) {
        String next = Objects.requireNonNull(value, "value");
        if (this.value.equals(next)) return;
        this.value = next;
        owner.sync();
    }

    @Override
    public void write(ValueOutput output) {
        output.putString(key, value);
    }

    @Override
    public void read(ValueInput input) {
        value = input.getStringOr(key, defaultValue);
    }
}
