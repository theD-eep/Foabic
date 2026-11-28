package dev.foabic.api.block;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Internal contract for a field stored on a {@link FoabicBlockEntity}. */
interface SyncedField {

    void write(ValueOutput output);

    void read(ValueInput input);
}
